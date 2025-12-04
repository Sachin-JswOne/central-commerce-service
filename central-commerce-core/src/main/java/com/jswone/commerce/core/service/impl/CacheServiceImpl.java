package com.jswone.commerce.core.service.impl;

import com.jsw.notification_common_model.email.NotificationConfig;
import com.jsw.notification_common_model.email.NotificationData;
import com.jsw.notification_common_model.email.NotificationModel;
import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.constants.CacheNames;
import com.jswone.commerce.core.entity.PurchasedSku;
import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.model.DistributedBuyAgainResponse;
import com.jswone.commerce.core.model.PipelineResult;
import com.jswone.commerce.core.service.CacheService;
import com.jswone.commerce.core.service.NotificationService;
import com.jswone.commerce.core.service.PurchasedSkuService;
import com.jswone.commerce.core.util.ApiResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.jswone.commerce.core.config.ProfileAwareCacheConfig.getCacheNameWithProfile;
import static com.jswone.commerce.core.constants.NotificationConstants.BUY_AGAIN_CACHE_WARM_UP_SUMMARY_MESSAGE;
import static com.jswone.commerce.core.constants.NotificationConstants.TEAMS;

@Slf4j
@Service
public class CacheServiceImpl implements CacheService {

    private static final int CHUNK_SIZE = 500;

    private static final int MAX_RETRIES = 3;

    private final RedisTemplate<String, Object> redisTemplate;

    private final CommerceValueConfig commerceValueConfig;

    private final PurchasedSkuService purchasedSkuService;

    private final BuyAgainServiceImplV2 buyAgainServiceV2;

    private final BuyAgainServiceImpl buyAgainService;

    private final NotificationService notificationService;

    public CacheServiceImpl(RedisTemplate<String, Object> redisTemplate, CommerceValueConfig commerceValueConfig, PurchasedSkuService purchasedSkuService, BuyAgainServiceImplV2 buyAgainServiceV2, BuyAgainServiceImpl buyAgainService, NotificationService notificationService) {
        this.redisTemplate = redisTemplate;
        this.commerceValueConfig = commerceValueConfig;
        this.purchasedSkuService = purchasedSkuService;
        this.buyAgainServiceV2 = buyAgainServiceV2;
        this.buyAgainService = buyAgainService;
        this.notificationService = notificationService;
    }

    /**
     * Bulk warm-up: load buy-again products for customers into cache.
     */
    @Override
    public void loadAllBuyAgainProductsForCustomersIntoCache() {

        log.info("BUY_AGAIN — Cache warm-up started...");

        List<PurchasedSku> purchasedSkuForAllCustomers =
                purchasedSkuService.fetchRecentlyPurchasedSkuForAllCustomers();

        if (purchasedSkuForAllCustomers == null || purchasedSkuForAllCustomers.isEmpty()) {
            log.info("BUY_AGAIN — Warm-up skipped: No purchase data found");
            return;
        }

        Map<String, List<PurchasedSku>> groupedSkusByCustomerId = purchasedSkuForAllCustomers.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(PurchasedSku::getCustomerId));

        int totalCustomers = groupedSkusByCustomerId.size();
        int totalSuccess = 0;
        int totalFailed = 0;

        List<Map.Entry<String, List<PurchasedSku>>> purchasedSkuList = new ArrayList<>(groupedSkusByCustomerId.entrySet());

        List<List<Map.Entry<String, List<PurchasedSku>>>> purchasedSkuChunks = chunkPurchasedSkus(purchasedSkuList);

        log.info("BUY_AGAIN — Processing {} customers in {} batches (chunkSize={})",
                totalCustomers, purchasedSkuChunks.size(), CHUNK_SIZE);

        List<String> globalFailedCustomerIds = new ArrayList<>();

        for (int batchIndex = 0; batchIndex < purchasedSkuChunks.size(); batchIndex++) {

            List<Map.Entry<String, List<PurchasedSku>>> purchasedSkuBatch = purchasedSkuChunks.get(batchIndex);
            log.info("BUY_AGAIN — Executing Purchased Sku batch {}/{} (size={})",
                    batchIndex + 1, purchasedSkuChunks.size(), purchasedSkuBatch.size());

            List<Map.Entry<String, List<PurchasedSku>>> remainingPurchasedSkus = new ArrayList<>(purchasedSkuBatch);
            int attempt = 0;

            // Retry failed keys only
            while (!remainingPurchasedSkus.isEmpty() && attempt <= MAX_RETRIES) {
                attempt++;

                log.info("BUY_AGAIN — Batch {} attempt {} – {} keys",
                        batchIndex + 1, attempt, remainingPurchasedSkus.size());

                PipelineResult result = executePipeline(remainingPurchasedSkus);

                totalSuccess += result.successCustomerIds.size();
                remainingPurchasedSkus = result.failedEntries;

                if (!remainingPurchasedSkus.isEmpty() && attempt < MAX_RETRIES) {
                    log.warn("BUY_AGAIN — Batch {} attempt {} had {} failures. Retrying...",
                            batchIndex + 1, attempt, remainingPurchasedSkus.size());
                }

                if (attempt == MAX_RETRIES && !remainingPurchasedSkus.isEmpty()) {
                    List<String> finalFailedIds = remainingPurchasedSkus.stream()
                            .map(Map.Entry::getKey)
                            .toList();

                    globalFailedCustomerIds.addAll(finalFailedIds);

                    totalFailed += remainingPurchasedSkus.size();
                    log.error("BUY_AGAIN — Batch {} FAILED after retries. Failed Customer IDs={}",
                            batchIndex + 1,
                            remainingPurchasedSkus.stream().map(Map.Entry::getKey).toList());
                }
            }
        }

        log.info("BUY_AGAIN — Warm-up completed. Total={}, Success={}, Failed={}",
                totalCustomers, totalSuccess, totalFailed);

        // Send Teams alert if failed customer ids happened
        if (totalFailed > 0) {
            sendNotificationRequest(globalFailedCustomerIds);
        }
    }

    private PipelineResult executePipeline(List<Map.Entry<String, List<PurchasedSku>>> purchasedSkus) {

        List<String> orderedCustomerIds = new ArrayList<>();
        List<Map.Entry<String, List<PurchasedSku>>> entryList = new ArrayList<>();

        // PIPELINE EXECUTION
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {

            for (Map.Entry<String, List<PurchasedSku>> entry : purchasedSkus) {

                String customerId = entry.getKey();
                List<PurchasedSku> skus = entry.getValue();

                try {
                    List<PurchasedSku> sortedPurchasedSkus = skus == null ? List.of() :
                            skus.stream()
                                    .filter(Objects::nonNull)
                                    .sorted(Comparator.comparing(PurchasedSku::getOrderPlacedDate,
                                            Comparator.nullsLast(Date::compareTo)).reversed())
                                    .toList();

                    DistributedBuyAgainResponse distributedBuyAgainResponse = commerceValueConfig.isCentralCatalogueServiceEnabled() ?
                            buyAgainServiceV2.getRecentPurchasedDistributed(sortedPurchasedSkus) :
                            buyAgainService.getRecentPurchasedDistributed(sortedPurchasedSkus);

                    DistributedBuyAgainResponse safeCopy = commerceValueConfig.isCentralCatalogueServiceEnabled() ?
                            buyAgainServiceV2.defensivelyCopyBuyAgainResponse(distributedBuyAgainResponse) :
                            buyAgainService.defensivelyCopyBuyAgainResponse(distributedBuyAgainResponse);

                    RedisSerializer<String> keySerializer =
                            (RedisSerializer<String>) redisTemplate.getKeySerializer();

                    RedisSerializer<Object> valueSerializer =
                            (RedisSerializer<Object>) redisTemplate.getValueSerializer();

                    String profile = commerceValueConfig.getRedisCacheProfile();

                    String buyAgainKey = getCacheNameWithProfile(profile, getCacheName()).concat(":" + customerId);

                    byte[] key = keySerializer.serialize(buyAgainKey);
                    byte[] val = valueSerializer.serialize(safeCopy);

                    // add order tracking
                    orderedCustomerIds.add(customerId);
                    entryList.add(entry);

                    connection.stringCommands().set(key, val);

                } catch (Exception e) {
                    log.error("BUY_AGAIN — Pre-pipeline failure for customer {}: {}", customerId, e.getMessage());
                }
            }
            return null;
        });

        PipelineResult pipelineResult = new PipelineResult();

        int count = Math.min(results.size(), orderedCustomerIds.size());
        for (int i = 0; i < count; i++) {

            Object res = results.get(i);
            String cid = orderedCustomerIds.get(i);
            Map.Entry<String, List<PurchasedSku>> entry = entryList.get(i);

            if (isRedisError(res)) {
                log.error("BUY_AGAIN — Redis pipeline error for {}: {}", cid, res);
                pipelineResult.failedEntries.add(entry);
            } else {
                pipelineResult.successCustomerIds.add(cid);
            }
        }
        return pipelineResult;
    }

    private String getCacheName() {
        return commerceValueConfig.isCentralCatalogueServiceEnabled() ?
                CacheNames.BUY_AGAIN_PRODUCTS_CACHE_PREFIX_V2 :
                CacheNames.BUY_AGAIN_PRODUCTS_CACHE_PREFIX;
    }

    private boolean isRedisError(Object res) {
        if (res == null) return true;
        if (res instanceof Exception) return true;
        return res.toString().startsWith("ERR") ||
                res.toString().toLowerCase().contains("error");
    }

    private <T> List<List<T>> chunkPurchasedSkus(List<T> list) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += CacheServiceImpl.CHUNK_SIZE) {
            chunks.add(list.subList(i, Math.min(list.size(), i + CacheServiceImpl.CHUNK_SIZE)));
        }
        return chunks;
    }

    private void sendNotificationRequest(List<String> failedCustomerIds) {
        try {
            String message = String.format(
                    BUY_AGAIN_CACHE_WARM_UP_SUMMARY_MESSAGE,
                    MAX_RETRIES,
                    failedCustomerIds.size(),
                    String.join(",\n", failedCustomerIds)
            );

            NotificationConfig config =
                    NotificationConfig.builder()
                            .workflowUrl(commerceValueConfig.getBuyAgainCacheWarmUpTeamsWorkflowUrl())
                            .build();

            NotificationModel<Object> notificationModel =
                    NotificationModel.builder()
                            .notificationConfig(config)
                            .notificationData(NotificationData.builder().message(message).build())
                            .channels(List.of(TEAMS))
                            .build();

            notificationService.sendNotificationRequest(notificationModel);
        } catch (Exception ex) {
            log.error("BUY_AGAIN — Failed sending notification for buy again cache warm up failures", ex);
        }
    }

    @Override
    public ApiResponse<Map<String, Object>> fetchBuyAgainKeys() {
        String buyAgainKeyPrefix = getCacheNameWithProfile(commerceValueConfig.getRedisCacheProfile(), getCacheName()).concat("*");
        log.info("Fetching Buy Again Redis keys with pattern: {}", buyAgainKeyPrefix);

        Set<String> keys = new HashSet<>();

        try {
            redisTemplate.execute((RedisCallback<Void>) connection -> {
                ScanOptions options = ScanOptions.scanOptions()
                        .match(buyAgainKeyPrefix)
                        .count(1000)
                        .build();

                try (Cursor<byte[]> cursor = connection.scan(options)) {
                    while (cursor.hasNext()) {
                        keys.add(new String(cursor.next()));
                    }
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Error while fetching buy-again Redis keys", e);
            return ApiResponseUtil.createErrorResponse(
                    "Unable to fetch Redis buy-again keys",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        Map<String, Object> result = new HashMap<>();
        result.put("count", keys.size());
        result.put("keys", keys);

        return ApiResponseUtil.createSuccessResponse(result, HttpStatus.OK);
    }

    @Override
    public ApiResponse<Map<String, Object>> deleteBuyAgainKeys() {
        String buyAgainKeyPrefix = getCacheNameWithProfile(commerceValueConfig.getRedisCacheProfile(), getCacheName()).concat("*");
        log.info("Deleting Buy Again Redis keys with pattern: {}", buyAgainKeyPrefix);

        AtomicInteger deleted = new AtomicInteger(0);

        try {
            redisTemplate.execute((RedisCallback<Void>) connection -> {
                ScanOptions options = ScanOptions.scanOptions()
                        .match(buyAgainKeyPrefix)
                        .count(1000)
                        .build();

                try (Cursor<byte[]> cursor = connection.scan(options)) {
                    while (cursor.hasNext()) {
                        byte[] key = cursor.next();
                        connection.del(key);
                        deleted.incrementAndGet();
                    }
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Error while deleting buy-again Redis keys", e);
            return ApiResponseUtil.createErrorResponse(
                    "Unable to delete Redis buy-again keys",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        Map<String, Object> response = new HashMap<>();
        response.put("deletedKeysPattern", buyAgainKeyPrefix);
        response.put("deletedKeyCount", deleted.get());

        return ApiResponseUtil.createSuccessResponse(response, HttpStatus.OK);
    }
}