package com.jswone.commerce.core.service;

import com.jswone.commerce.core.model.centralCatalogue.ProductTypeData;
import com.jswone.commerce.core.model.centralCatalogue.QuantityCard;

import java.util.Map;
import java.util.Set;


public interface ProductTypeService {

    /**
     * Fetch product type data for multiple product type IDs with Redis caching.
     * <p>
     * Flow:
     * 1. Check Redis cache for each product type ID
     * 2. Collect cache misses
     * 3. If misses exist, make bulk API call to fetch all missing IDs
     * 4. Cache each fetched product type individually in Redis
     * 5. Merge cached + fetched data and return
     */
    Map<String, ProductTypeData> getProductTypes(Set<String> productTypeIds, String storefront);

    /**
     * Get rank 0 quantity card for a specific product type ID.
     */
    QuantityCard getQuantityCardForProductType(String productTypeId, String storefront);

    /**
     * Get rank 0 quantity cards for multiple product type IDs.
     */
    Map<String, QuantityCard> getQuantityCardsForProductTypes(Set<String> productTypeIds, String storefront);
}
