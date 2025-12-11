package com.jswone.commerce.core.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProfileAwareCacheConfig {

    private final CommerceValueConfig commerceValueConfig;

    public ProfileAwareCacheConfig(CommerceValueConfig commerceValueConfig) {
        this.commerceValueConfig = commerceValueConfig;
    }

    public static String getCacheNameWithProfile(String cacheProfile, String cacheName) {
        return cacheProfile.isBlank() ? cacheName : cacheProfile + ":" + cacheName;
    }

    @Bean
    public ProfileAwareCacheResolver getProfileAwareCacheResolver(CacheManager cacheManager) {
        return new ProfileAwareCacheResolver(cacheManager, commerceValueConfig);
    }
}