package com.jswone.commerce.core.service.impl;

import com.jsw.notification_common_model.email.NotificationConfig;
import com.jsw.notification_common_model.email.NotificationData;
import com.jsw.notification_common_model.email.NotificationModel;
import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.constants.CacheNames;
import com.jswone.commerce.core.entity.PurchasedSku;
import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.model.BuyAgainResponse;
import com.jswone.commerce.core.model.PipelineResult;
import com.jswone.commerce.core.service.CacheService;
import com.jswone.commerce.core.service.NotificationService;
import com.jswone.commerce.core.service.PurchasedSkuService;
import com.jswone.commerce.core.util.ApiResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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
import static com.jswone.commerce.core.constants.NotificationConstants.*;

@Slf4j
@Service
public class CacheServiceImpl implements CacheService {

    private static final int MAX_RETRIES = 3;

    private final RedisTemplate<String, Object> redisTemplate;

    private final CommerceValueConfig commerceValueConfig;

    private final PurchasedSkuService purchasedSkuService;

    private final BuyAgainServiceImplV2 buyAgainServiceV2;

    private final BuyAgainServiceImpl buyAgainService;

    private final NotificationService notificationService;

    private final CacheManager cacheManager;

    public CacheServiceImpl(RedisTemplate<String, Object> redisTemplate, CommerceValueConfig commerceValueConfig, PurchasedSkuService purchasedSkuService, BuyAgainServiceImplV2 buyAgainServiceV2, BuyAgainServiceImpl buyAgainService, NotificationService notificationService, CacheManager cacheManager) {
        this.redisTemplate = redisTemplate;
        this.commerceValueConfig = commerceValueConfig;
        this.purchasedSkuService = purchasedSkuService;
        this.buyAgainServiceV2 = buyAgainServiceV2;
        this.buyAgainService = buyAgainService;
        this.notificationService = notificationService;
        this.cacheManager = cacheManager;
    }

    private boolean isRedisEnabled() {
        return commerceValueConfig.isRedisEnabled();
    }

    /**
     * Bulk warm-up: load buy-again products for customers into cache.
     */
    @Override
    public void loadAllBuyAgainProductsForCustomersIntoCache() {

        log.info("BUY_AGAIN — Cache warm-up started...");

        int chunkSize = commerceValueConfig.getBuyAgainCacheChunkSize();

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

        List<List<Map.Entry<String, List<PurchasedSku>>>> purchasedSkuChunks = chunkPurchasedSkus(purchasedSkuList, chunkSize);

        log.info("BUY_AGAIN — Processing {} customers in {} batches (chunkSize={})",
                totalCustomers, purchasedSkuChunks.size(), chunkSize);

        List<String> globalFailedCustomerIds = new ArrayList<>();

        for (int batchIndex = 0; batchIndex < purchasedSkuChunks.size(); batchIndex++) {

            List<Map.Entry<String, List<PurchasedSku>>> purchasedSkuBatch = purchasedSkuChunks.get(batchIndex);
            log.info("BUY_AGAIN — Executing Purchased Sku batch {}/{} (size={})",
                    batchIndex + 1, purchasedSkuChunks.size(), purchasedSkuBatch.size());

            List<Map.Entry<String, List<PurchasedSku>>> remainingPurchasedSkus = new ArrayList<>(purchasedSkuBatch);
            int attempt = 0;

            // Retry failed keys
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

        sendNotificationRequest(totalCustomers, totalSuccess, globalFailedCustomerIds);
    }

    private PipelineResult executePipeline(List<Map.Entry<String, List<PurchasedSku>>> purchasedSkus) {

        if (!isRedisEnabled()) {
            log.info("BUY_AGAIN — Caffeine cache enabled, skipping Redis pipeline");
            return executeCaffeineCache(purchasedSkus);
        }

        List<String> orderedCustomerIds = new ArrayList<>();
        List<Map.Entry<String, List<PurchasedSku>>> entryList = new ArrayList<>();

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

                    BuyAgainResponse buyAgainResponse = commerceValueConfig.isCentralCatalogueServiceEnabled() ?
                            buyAgainServiceV2.getRecentPurchased(sortedPurchasedSkus) :
                            buyAgainService.getRecentPurchasedDistributed(sortedPurchasedSkus);

                    BuyAgainResponse defensiveBuyAgainResponse = commerceValueConfig.isCentralCatalogueServiceEnabled() ?
                            buyAgainServiceV2.defensivelyCopyBuyAgainResponse(buyAgainResponse) :
                            buyAgainService.defensivelyCopyBuyAgainResponse(buyAgainResponse);

                    RedisSerializer<String> keySerializer =
                            (RedisSerializer<String>) redisTemplate.getKeySerializer();

                    RedisSerializer<Object> valueSerializer =
                            (RedisSerializer<Object>) redisTemplate.getValueSerializer();

                    String profile = commerceValueConfig.getRedisCacheProfile();

                    String buyAgainKey = getCacheNameWithProfile(profile, getCacheName()).concat(":" + customerId);

                    byte[] key = keySerializer.serialize(buyAgainKey);
                    byte[] val = valueSerializer.serialize(defensiveBuyAgainResponse);

                    // add order tracking
                    orderedCustomerIds.add(customerId);
                    entryList.add(entry);

                    connection.stringCommands().set(key, val);

                } catch (Exception e) {
                    log.error("BUY_AGAIN — Pre-pipeline failure for customerId: {} with message: {}", customerId, e.getMessage());
                }
            }
            return null;
        });

        PipelineResult pipelineResult = new PipelineResult();

        int count = Math.min(results.size(), orderedCustomerIds.size());
        for (int i = 0; i < count; i++) {

            Object res = results.get(i);
            String customerId = orderedCustomerIds.get(i);
            Map.Entry<String, List<PurchasedSku>> entry = entryList.get(i);

            if (isRedisError(res)) {
                log.error("BUY_AGAIN — Redis pipeline error for customerId: {} with res:{}", customerId, res);
                pipelineResult.failedEntries.add(entry);
            } else {
                pipelineResult.successCustomerIds.add(customerId);
            }
        }
        return pipelineResult;
    }

    private PipelineResult executeCaffeineCache(List<Map.Entry<String, List<PurchasedSku>>> purchasedSkus) {

        PipelineResult pipelineResult = new PipelineResult();

        String profile = commerceValueConfig.getRedisCacheProfile();
        String cacheName = getCacheNameWithProfile(profile, getCacheName());

        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.error("BUY_AGAIN — Caffeine cache not found for name={}", cacheName);
            pipelineResult.failedEntries.addAll(purchasedSkus);
            return pipelineResult;
        }

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

                BuyAgainResponse buyAgainResponse = commerceValueConfig.isCentralCatalogueServiceEnabled() ?
                        buyAgainServiceV2.getRecentPurchased(sortedPurchasedSkus) :
                        buyAgainService.getRecentPurchasedDistributed(sortedPurchasedSkus);

                BuyAgainResponse defensiveBuyAgainResponse = commerceValueConfig.isCentralCatalogueServiceEnabled() ?
                        buyAgainServiceV2.defensivelyCopyBuyAgainResponse(buyAgainResponse) :
                        buyAgainService.defensivelyCopyBuyAgainResponse(buyAgainResponse);

                String buyAgainCacheKey = cacheName.concat(":" + customerId);

                cache.put(buyAgainCacheKey, defensiveBuyAgainResponse);
                pipelineResult.successCustomerIds.add(customerId);

            } catch (Exception e) {
                log.error("BUY_AGAIN — Caffeine cache failure for customerId: {} with message: {}", customerId, e.getMessage());
                pipelineResult.failedEntries.add(entry);
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

    private <T> List<List<T>> chunkPurchasedSkus(List<T> list, int chunkSize) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            chunks.add(list.subList(i, Math.min(list.size(), i + chunkSize)));
        }
        return chunks;
    }

    private void sendNotificationRequest(int totalCustomers, int success, List<String> failedCustomerIds) {
        try {
            String message = String.format(
                    commerceValueConfig.isCentralCatalogueServiceEnabled() ?
                            BUY_AGAIN_CACHE_WARM_UP_SUMMARY_MESSAGE : BUY_AGAIN_CT_CACHE_WARM_UP_SUMMARY_MESSAGE,
                    totalCustomers,
                    success,
                    failedCustomerIds.size(),
                    String.join(",\n", failedCustomerIds.isEmpty() ? List.of("None") : failedCustomerIds)
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

        if (!isRedisEnabled()) {
            return fetchKeysFromCaffeineCache();
        }

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

    private ApiResponse<Map<String, Object>> fetchKeysFromCaffeineCache() {
        log.info("Caffeine cache enabled, fetching keys from Caffeine cache");

        String cacheNameWithProfile =
                getCacheNameWithProfile(commerceValueConfig.getRedisCacheProfile(), getCacheName());
        Cache cache = cacheManager.getCache(cacheNameWithProfile);
        if (cache == null) {
            log.error("Caffeine cache not found for name={}", cacheNameWithProfile);
            return ApiResponseUtil.createErrorResponse(
                    "Caffeine cache not found for Buy-Again",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        Map<String, Object> result = new HashMap<>();
        Set<String> keys = new HashSet<>();

        Object nativeCache = cache.getNativeCache();
        try {
            if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache<?, ?> caffeineCache) {
                Map<?, ?> map = caffeineCache.asMap();
                for (Object key : map.keySet()) {
                    keys.add(String.valueOf(key));
                }
            } else if (nativeCache instanceof Map<?, ?> map) {
                for (Object key : map.keySet()) {
                    keys.add(String.valueOf(key));
                }
            } else {
                log.warn("Unsupported native cache type for Caffeine: {}", nativeCache.getClass());
            }
        } catch (Exception e) {
            log.error("Error while reading Caffeine cache keys", e);
            return ApiResponseUtil.createErrorResponse(
                    "Unable to fetch Caffeine Buy-Again keys",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        result.put("count", keys.size());
        result.put("keys", keys);

        return ApiResponseUtil.createSuccessResponse(result, HttpStatus.OK);
    }

    @Override
    public ApiResponse<Map<String, Object>> deleteBuyAgainKeys() {
        String buyAgainKeyPrefix = getCacheNameWithProfile(commerceValueConfig.getRedisCacheProfile(), getCacheName()).concat("*");
        log.info("Deleting Buy Again Redis keys with pattern: {}", buyAgainKeyPrefix);

        if (!isRedisEnabled()) {
            log.info("Caffeine cache enabled, deleting keys from Caffeine cache");
            return deleteKeysFromCaffiene(buyAgainKeyPrefix);
        }

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

    private ApiResponse<Map<String, Object>> deleteKeysFromCaffiene(String buyAgainKeyPrefix) {
        String cacheNameWithProfile =
                getCacheNameWithProfile(commerceValueConfig.getRedisCacheProfile(), getCacheName());

        Cache cache = cacheManager.getCache(cacheNameWithProfile);
        if (cache == null) {
            log.error("Caffeine cache not found for name={}", cacheNameWithProfile);
            return ApiResponseUtil.createErrorResponse(
                    "Caffeine cache not found for Buy-Again",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        int deletedCount = 0;

        Object nativeCache = cache.getNativeCache();
        try {
            if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache<?, ?> caffeineCache) {
                Map<?, ?> map = caffeineCache.asMap();
                deletedCount = map.size();
                cache.clear(); // clears all entries
            } else if (nativeCache instanceof Map<?, ?> map) {
                deletedCount = map.size();
                cache.clear();
            } else {
                log.warn("Unsupported native cache type for Caffeine during delete: {}", nativeCache.getClass());
                cache.clear();
            }
        } catch (Exception e) {
            log.error("Error while deleting Caffeine cache keys", e);
            return ApiResponseUtil.createErrorResponse(
                    "Unable to delete Caffeine Buy-Again keys",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        Map<String, Object> response = new HashMap<>();
        response.put("deletedKeysPattern", buyAgainKeyPrefix);
        response.put("deletedKeyCount", deletedCount);

        return ApiResponseUtil.createSuccessResponse(response, HttpStatus.OK);
    }
}