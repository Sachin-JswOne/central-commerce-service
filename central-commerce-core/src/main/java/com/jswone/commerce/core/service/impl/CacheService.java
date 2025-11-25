package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.constants.CacheNames;
import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.util.ApiResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisAccessor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.jswone.commerce.core.config.ProfileAwareCacheConfig.getCacheNameWithProfile;

@Slf4j
@Service
public class CacheService {
    private final CacheManager cacheManager;

    private final RedisTemplate<String, Object> redisTemplate;

    private final CommerceValueConfig commerceValueConfig;

    public CacheService(CacheManager cacheManager, @Autowired(required = false) RedisTemplate<String, Object> redisTemplate, CommerceValueConfig commerceValueConfig) {
        this.cacheManager = cacheManager;
        this.redisTemplate = redisTemplate;
        this.commerceValueConfig = commerceValueConfig;
    }

    public Map<String, Integer> getAllCache() {

        Map<String, Integer> result = new HashMap<>();

        RedisConnection connection =
                Optional.of(redisTemplate)
                        .map(RedisAccessor::getConnectionFactory)
                        .map(RedisConnectionFactory::getConnection)
                        .orElseThrow(
                                () -> {
                                    log.error("Failed to acquire redis connection");
                                    return new RuntimeException("Redis connection acquisition failed");
                                });

        try {
            for (String cacheName : cacheManager.getCacheNames()) {
                Set<byte[]> keys = connection.keys(cacheName.concat(":*").getBytes());
                result.put(
                        cacheName.split(getCacheNameWithProfile(commerceValueConfig.getRedisCacheProfile(), CacheNames.CENTRAL_COMMERCE_SERVICE_CACHE_PREFIX))[1],
                        Objects.isNull(keys) ? 0 : keys.size());
            }
        } catch (Exception ex) {
            log.error("Exception occurred when fetching cache");
        } finally {
            connection.close();
        }

        return result;
    }

    public ApiResponse<Map<String, Object>> deleteBuyAgainCache() {

        if (!commerceValueConfig.isRedisCacheManagerEnabled()) {
            return ApiResponseUtil.createErrorResponse(
                    "Redis cache manager disabled", HttpStatus.BAD_REQUEST);
        }

        String profile = commerceValueConfig.getRedisCacheProfile();

        // Construct prefix for Buy Again cache
        String buyAgainPrefix = getCacheNameWithProfile(
                profile,
                CacheNames.BUY_AGAIN_PRODUCTS
        );

        log.info("Deleting Redis Buy Again cache. Prefix={}", buyAgainPrefix);

        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(buyAgainPrefix + "*")
                .count(Integer.MAX_VALUE)
                .build();

        int deletedCount = 0;

        try (Cursor<byte[]> cursor =
                     redisTemplate.getConnectionFactory().getConnection().scan(scanOptions)) {

            while (cursor.hasNext()) {
                String key = new String(cursor.next());
                redisTemplate.delete(key);
                deletedCount++;
            }

        } catch (Exception e) {
            log.error("Error occurred while clearing Buy Again cache", e);
            return ApiResponseUtil.createErrorResponse(
                    "Error clearing Buy Again redis cache", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        log.info("Deleted {} keys from Buy Again redis cache", deletedCount);

        // Response format
        Map<String, Object> response = new HashMap<>();
        response.put("buyAgainCacheDeletedKeys", deletedCount);

        return ApiResponseUtil.createSuccessResponse(response, HttpStatus.OK);
    }
}
