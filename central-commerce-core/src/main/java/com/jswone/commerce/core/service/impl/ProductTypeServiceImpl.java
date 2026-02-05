package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.constants.CacheNames;
import com.jswone.commerce.core.model.centralCatalogue.ProductTypeData;
import com.jswone.commerce.core.model.centralCatalogue.QuantityCard;
import com.jswone.commerce.core.model.request.ProductTypeBulkRequest;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductTypeBulkResponse;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.service.ProductTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.jswone.commerce.core.config.ProfileAwareCacheConfig.getCacheNameWithProfile;


@Service
@Slf4j
public class ProductTypeServiceImpl implements ProductTypeService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CentralCatalogueClient centralCatalogueClient;
    private final CommerceValueConfig commerceValueConfig;

    // Cache TTL: 24 hours
    private static final long CACHE_TTL_HOURS = 24;

    public ProductTypeServiceImpl(
            RedisTemplate<String, Object> redisTemplate,
            CentralCatalogueClient centralCatalogueClient,
            CommerceValueConfig commerceValueConfig) {
        this.redisTemplate = redisTemplate;
        this.centralCatalogueClient = centralCatalogueClient;
        this.commerceValueConfig = commerceValueConfig;
    }

    @Override
    public Map<String, ProductTypeData> getProductTypes(Set<String> productTypeIds, String storefront) {
        if (productTypeIds == null || productTypeIds.isEmpty()) {
            log.debug("ProductTypeService - No product type IDs provided");
            return Collections.emptyMap();
        }

        log.info("ProductTypeService - Fetching {} product types for storefront: {}",
                productTypeIds.size(), storefront);

        Map<String, ProductTypeData> resultMap = new HashMap<>();
        Set<String> cacheMisses = new HashSet<>();

        // Step 1: Check Redis cache for each product type ID
        for (String productTypeId : productTypeIds) {
            String cacheKey = buildCacheKey(storefront, productTypeId);
            ProductTypeData cachedData = (ProductTypeData) redisTemplate.opsForValue().get(cacheKey);

            if (cachedData != null) {
                log.debug("ProductTypeService - Cache HIT for productTypeId: {}", productTypeId);
                resultMap.put(productTypeId, cachedData);
            } else {
                log.debug("ProductTypeService - Cache MISS for productTypeId: {}", productTypeId);
                cacheMisses.add(productTypeId);
            }
        }

        log.info("ProductTypeService - Cache hits: {}, Cache misses: {}",
                resultMap.size(), cacheMisses.size());

        // Step 2: Fetch missing product types via bulk API
        if (!cacheMisses.isEmpty()) {
            Map<String, ProductTypeData> fetchedData = fetchProductTypesFromAPI(cacheMisses, storefront);

            // Step 3: Cache the fetched data
            cacheProductTypes(fetchedData, storefront);

            // Step 4: Merge with existing results
            resultMap.putAll(fetchedData);
        }

        log.info("ProductTypeService - Returning {} product types", resultMap.size());
        return resultMap;
    }

    @Override
    public QuantityCard getQuantityCardForProductType(String productTypeId, String storefront) {
        if (productTypeId == null) {
            return null;
        }

        Map<String, ProductTypeData> productTypeMap = getProductTypes(Set.of(productTypeId), storefront);
        ProductTypeData productTypeData = productTypeMap.get(productTypeId);

        return extractRank0QuantityCard(productTypeData);
    }

    @Override
    public Map<String, QuantityCard> getQuantityCardsForProductTypes(
            Set<String> productTypeIds,
            String storefront) {
        Map<String, ProductTypeData> productTypeMap = getProductTypes(productTypeIds, storefront);

        return productTypeMap.entrySet().stream()
                .map(entry -> {
                    String productTypeId = entry.getKey();
                    QuantityCard card = extractRank0QuantityCard(entry.getValue());
                    return card != null ? Map.entry(productTypeId, card) : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Fetch product types from Central Catalogue API
     */
    private Map<String, ProductTypeData> fetchProductTypesFromAPI(
            Set<String> productTypeIds,
            String storefront) {
        try {
            log.info("ProductTypeService - Calling Central Catalogue bulk API for {} product type IDs",
                    productTypeIds.size());

            ProductTypeBulkRequest  request = new ProductTypeBulkRequest(productTypeIds, storefront);
            ProductTypeBulkResponse response = centralCatalogueClient.bulkTypeIdResponse(request);

            if (response == null || response.getData() == null ||
                    response.getData().getProductTypeDetail() == null) {
                log.warn("ProductTypeService - Empty response from Central Catalogue API");
                return Collections.emptyMap();
            }

            Map<String, ProductTypeData> fetchedData = response.getData().getProductTypeDetail();
            log.info("ProductTypeService - Successfully fetched {} product types from API",
                    fetchedData.size());

            return fetchedData;

        } catch (Exception e) {
            log.error("ProductTypeService - Error fetching product types from API: {}",
                    e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Cache product type data in Redis
     */
    private void cacheProductTypes(Map<String, ProductTypeData> productTypeMap, String storefront) {
        if (productTypeMap == null || productTypeMap.isEmpty()) {
            return;
        }

        for (Map.Entry<String, ProductTypeData> entry : productTypeMap.entrySet()) {
            String productTypeId = entry.getKey();
            ProductTypeData productTypeData = entry.getValue();
            String cacheKey = buildCacheKey(storefront, productTypeId);

            try {
                redisTemplate.opsForValue().set(cacheKey, productTypeData, CACHE_TTL_HOURS, TimeUnit.HOURS);
                log.debug("ProductTypeService - Cached product type: {}", productTypeId);
            } catch (Exception e) {
                log.error("ProductTypeService - Error caching product type {}: {}",
                        productTypeId, e.getMessage());
            }
        }

        log.info("ProductTypeService - Cached {} product types in Redis", productTypeMap.size());
    }

    /**
     * Build Redis cache key
     */
    private String buildCacheKey(String storefront, String productTypeId) {
        String cachePrefix = getCacheNameWithProfile(
                commerceValueConfig.getRedisCacheProfile(),
                CacheNames.SEO_PRODUCT_TYPES);
        return String.format("%s:%s:%s", cachePrefix, storefront, productTypeId);
    }

    /**
     * Extract rank 0 quantity card from product type data
     */
    private QuantityCard extractRank0QuantityCard(ProductTypeData productTypeData) {
        if (productTypeData == null || productTypeData.getQuantityCards() == null) {
            return null;
        }

        return productTypeData.getQuantityCards().stream()
                .filter(card -> card.getRank() == 0)
                .findFirst()
                .orElse(null);
    }
}
