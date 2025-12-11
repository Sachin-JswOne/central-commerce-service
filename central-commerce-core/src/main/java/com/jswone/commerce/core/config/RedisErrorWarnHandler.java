package com.jswone.commerce.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisErrorWarnHandler extends SimpleCacheErrorHandler {
    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn(
                "Cache GET failed for key '{}'. Skipping cache. Reason: {}", key, exception.getMessage());
    }

    @Override
    public void handleCachePutError(
            RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn(
                "Cache PUT failed for key '{}'. Skipping cache. Reason: {}", key, exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn(
                "Cache EVICT failed for key '{}'. Skipping cache. Reason: {}", key, exception.getMessage());
    }
}
