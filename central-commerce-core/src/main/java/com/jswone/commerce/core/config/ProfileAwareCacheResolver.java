package com.jswone.commerce.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

@Configuration
@EnableCaching
public class ProfileAwareCacheResolver extends SimpleCacheResolver {

    private final CommerceValueConfig commerceValueConfig;

    @Value("${redis.profile}")
    private String cacheProfile;

    public ProfileAwareCacheResolver(CacheManager cacheManager, CommerceValueConfig commerceValueConfig) {
        super(cacheManager);
        this.commerceValueConfig = commerceValueConfig;
    }

    // @Cacheable attribute 'value' accepts constant data only, and we have multiple profiles, so we are
    // adding profile as prefix of cacheNames
    @Override
    protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
        Collection<String> baseNames = super.getCacheNames(context);
        if (baseNames == null || baseNames.isEmpty()) {
            return Collections.emptyList();
        }
        String cacheProfile = commerceValueConfig.getRedisCacheProfile();
        return baseNames.stream()
                .map(name -> cacheProfile.isBlank() ? name : cacheProfile + ":" + name)
                .collect(Collectors.toList());
    }
}

