package com.jswone.commerce.core.service;

import java.util.Set;

/**
 * Service for managing location master data (states and districts).
 * Data is cached in Redis for performance with fallback to API on cache miss.
 */
public interface LocationMasterService {

    Set<String> getAllStates();

    Set<String> getAllDistricts();

    /**
     * Check if a seo location (state or district) is valid.
     * 
     * @param location The location name to validate
     * @return true if location exists as a state or district, false otherwise
     */
    boolean isValidSeoLocation(String location);

    void warmCache();
}
