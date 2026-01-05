package com.jswone.commerce.core.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.config.ProfileAwareCacheConfig;
import com.jswone.commerce.core.constants.CacheNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@EnableCaching
@Configuration
@ConditionalOnProperty(name = "central.commerce.redis.cache_manager.enable", havingValue = "false")
@Slf4j
public class CaffieneCacheConfig {

    private final CommerceValueConfig commerceValueConfig;

    public CaffieneCacheConfig(CommerceValueConfig commerceValueConfig) {
        this.commerceValueConfig = commerceValueConfig;
    }

    @Bean
    public CacheManager cacheManager() {
        log.info("-----ENABLING CAFFEINE AS SPRING CACHE MANAGER-----");
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        cacheManager.registerCustomCache(
                ProfileAwareCacheConfig.getCacheNameWithProfile(commerceValueConfig.getRedisCacheProfile(), CacheNames.BUY_AGAIN_PRODUCTS_CACHE_PREFIX_V2),
                Caffeine.newBuilder().expireAfterWrite(24L, TimeUnit.HOURS).build());

        cacheManager.registerCustomCache(
                ProfileAwareCacheConfig.getCacheNameWithProfile(commerceValueConfig.getRedisCacheProfile(), CacheNames.BUY_AGAIN_PRODUCTS_CACHE_PREFIX),
                Caffeine.newBuilder().expireAfterWrite(24L, TimeUnit.HOURS).build());

        return cacheManager;
    }
}
