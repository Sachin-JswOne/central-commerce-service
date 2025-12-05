package com.jswone.commerce.core.service;

import com.jswone.commerce.core.model.ApiResponse;

import java.util.Map;

public interface CacheService {

    void loadAllBuyAgainProductsForCustomersIntoCache();

    ApiResponse<Map<String, Object>> fetchBuyAgainKeys();

    ApiResponse<Map<String, Object>> deleteBuyAgainKeys();
}
