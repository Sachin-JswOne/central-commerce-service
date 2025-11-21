package com.jswone.commerce.core.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.jswone.commerce.core.constants.CacheNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${redis.profile}")
    private String cacheProfile;

    @Bean
    public CacheManager cacheManager() {
        log.info("-----ENABLING CAFFEINE AS SPRING CACHE MANAGER-----");
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        cacheManager.registerCustomCache(
                ProfileAwareCacheConfig.getCacheNameWithProfile(cacheProfile, CacheNames.BUY_AGAIN_PRODUCTS),
                Caffeine.newBuilder().expireAfterWrite(24L, TimeUnit.HOURS).build());
        return cacheManager;
    }
}
