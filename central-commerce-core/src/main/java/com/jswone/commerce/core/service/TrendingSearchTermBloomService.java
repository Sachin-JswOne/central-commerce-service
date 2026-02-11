package com.jswone.commerce.core.service;

import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.constants.CacheNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPooled;

import java.util.HashSet;
import java.util.Set;

import static com.jswone.commerce.core.config.ProfileAwareCacheConfig.getCacheNameWithProfile;

@Component
@Slf4j
@RequiredArgsConstructor
public class TrendingSearchTermBloomService {

    private static final String DEDUPE_PREFIX = CacheNames.DEDUPE_TRENDING_SEARCHES_CACHE_PREFIX;
    private static final String BARRED_SET = CacheNames.BARRED_TRENDING_SEARCHES_CACHE_PREFIX;

    private final JedisPooled jedisPooled;
    private final CacheManager cacheManager;
    private final CommerceValueConfig commerceValueConfig;

    private boolean isDuplicate(String redisKey) {
        Cache cache = getDupeTrendingSearchesCache();
        Cache.ValueWrapper wrapper = cache.get(redisKey);
        return wrapper != null;
    }

    public boolean addToBloom(String redisKey) {
        Cache cache = getDupeTrendingSearchesCache();
        if (!isDuplicate(redisKey)) {
            cache.put(redisKey, "1");
            return true;
        } else {
            return false;
        }
    }

    public boolean isBarred(String normalizedQuery) {
        Set<String> barredTerms = getBarredTerms();
        return barredTerms != null && barredTerms.contains(normalizedQuery);
    }

    public Set<String> getBarredTerms() {
        Cache cache = getBarredTrendingSearchesCache();
        Cache.ValueWrapper wrapper = cache.get("terms");
        if (wrapper != null) {
            return (HashSet<String>) wrapper.get();
        }
        return new HashSet<>();
    }

    public void barTerm(Set<String> normalizedQuery) {
        Cache cache = getBarredTrendingSearchesCache();
        Cache.ValueWrapper wrapper = cache.get("terms");
        if (wrapper != null) {
            Set<String> barredTerms = (HashSet<String>) wrapper.get();
            barredTerms.addAll(normalizedQuery);
            cache.put("terms", barredTerms);
        } else {
            log.debug("No Data found for clear recent search");
            cache.put("terms", normalizedQuery);
        }
    }

    private Cache getBarredTrendingSearchesCache() {
        Cache cache = cacheManager.getCache(getCacheNameWithProfile(commerceValueConfig.getRedisCacheProfile(), BARRED_SET));
        if (cache == null) {
            log.error("Recent Searches - Cache '{}' not found in CacheConfig", BARRED_SET);
            throw new IllegalStateException("Cache not configured: " + BARRED_SET);
        }
        return cache;
    }

    private Cache getDupeTrendingSearchesCache() {
        Cache cache = cacheManager.getCache(getCacheNameWithProfile(commerceValueConfig.getRedisCacheProfile(), DEDUPE_PREFIX));
        if (cache == null) {
            log.error("Recent Searches - Cache '{}' not found in CacheConfig", BARRED_SET);
            throw new IllegalStateException("Cache not configured: " + BARRED_SET);
        }
        return cache;
    }

}
