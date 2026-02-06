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
     * 3. If it misses exist, make bulk API call to fetch all missing IDs and caches them
     */
    Map<String, ProductTypeData> getProductTypes(Set<String> productTypeIds, String storefront);

    QuantityCard getQuantityCardForProductType(String productTypeId, String storefront);

    Map<String, QuantityCard> getQuantityCardsForProductTypes(Set<String> productTypeIds, String storefront);
}
