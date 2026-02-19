package com.jswone.commerce.core.service;

import java.util.Set;

/**
 * Service for managing location master data (states and districts).
 * Data is cached in Redis for performance with fallback to API on cache miss.
 */
public interface LocationMasterService {

    Set<String> getAllStates();

    Set<String> getAllDistricts();

    Set<String> getDistrictsForState(String state);

    boolean isValidSeoLocation(String location);

    void warmCache();

    void initCategoryLocations(String categoryId);

    void addCategoryLocation(String categoryId, String location);

    Set<String> getCategoryLocations(String categoryId);

    void clearCategoryLocations(String categoryId);
}
