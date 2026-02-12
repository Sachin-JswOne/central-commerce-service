package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.constants.CacheNames;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.service.LocationMasterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.jswone.commerce.core.config.ProfileAwareCacheConfig.getCacheNameWithProfile;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationMasterServiceImpl implements LocationMasterService {

    private final CentralCatalogueClient centralCatalogueClient;
    private final CacheManager cacheManager;
    private final CommerceValueConfig commerceValueConfig;

    @Override
    public Set<String> getAllStates() {
        Map<String, Set<String>> locations = getLocationsFromCacheOrApi();
        return locations.keySet();
    }

    @Override
    public Set<String> getAllDistricts() {
        Map<String, Set<String>> locations = getLocationsFromCacheOrApi();
        return locations.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValidSeoLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            return false;
        }

        Map<String, Set<String>> locations = getLocationsFromCacheOrApi();
        String normalizedLocation = location.trim().toUpperCase();

        // Check if it's a state
        if (locations.containsKey(normalizedLocation)) {
            return true;
        }

        // Check if it's a district
        return locations.values().stream()
                .anyMatch(districts -> districts.contains(normalizedLocation));
    }

    @Override
    public void warmCache() {
        try {
            log.info("Warming location cache at startup...");
            Map<String, Set<String>> locations = centralCatalogueClient.getAllServiceableLocations();

            if (locations == null || locations.isEmpty()) {
                log.warn("No locations returned from API during cache warming");
                return;
            }

            saveLocationsToCache(locations);
            log.info("Successfully warmed location cache with {} states and {} districts",
                    locations.size(),
                    locations.values().stream().mapToLong(Set::size).sum());
        } catch (Exception e) {
            log.error("Failed to warm location cache: {}", e.getMessage(), e);
            // Don't throw - application should start even if cache warming fails
        }
    }

    /**
     * Get locations from cache, or fetch from API if cache miss.
     */
    private Map<String, Set<String>> getLocationsFromCacheOrApi() {
        try {
            Cache cache = getLocationCache();

            // Try to get from cache
            Map<String, Set<String>> cachedData = cache.get("all", Map.class);

            if (cachedData != null) {
                log.debug("Location data found in cache");
                return cachedData;
            }

            log.info("Location cache miss - fetching from API");

            // Cache miss - fetch from API
            Map<String, Set<String>> locations = centralCatalogueClient.getAllServiceableLocations();

            if (locations != null && !locations.isEmpty()) {
                saveLocationsToCache(locations);
            }

            return locations != null ? locations : Collections.emptyMap();

        } catch (Exception e) {
            log.error("Error retrieving locations: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Save locations to Redis cache via CacheManager.
     */
    private void saveLocationsToCache(Map<String, Set<String>> locations) {
        try {
            Cache cache = getLocationCache();
            cache.put("all", locations);
            log.debug("Saved location data to cache");
        } catch (Exception e) {
            log.error("Failed to save locations to cache: {}", e.getMessage(), e);
        }
    }

    /**
     * Get the location cache from CacheManager.
     */
    private Cache getLocationCache() {
        String cacheName = getCacheNameWithProfile(
                commerceValueConfig.getRedisCacheProfile(),
                CacheNames.LOCATION_MASTER_ALL);

        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.error("Location cache '{}' not found in CacheManager", cacheName);
            throw new CentralCommerceServiceException("Cache not configured: " + cacheName);
        }
        return cache;
    }
}
