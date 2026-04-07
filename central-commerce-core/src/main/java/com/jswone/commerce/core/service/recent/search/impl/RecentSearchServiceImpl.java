package com.jswone.commerce.core.service.recent.search.impl;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.constants.CacheNames;
import com.jswone.commerce.core.model.elastic.index.RecentSearchIndex;
import com.jswone.commerce.core.repository.elastic.RecentSearchElasticIndexRepository;
import com.jswone.commerce.core.service.recent.search.RecentSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.jswone.commerce.core.config.ProfileAwareCacheConfig.getCacheNameWithProfile;
import static com.jswone.commerce.core.constants.JWTConstants.USER_ID_CLAIM;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecentSearchServiceImpl implements RecentSearchService {

    private final RecentSearchElasticIndexRepository recentSearchElasticIndexRepository;
    private final CacheManager cacheManager;
    private final CommerceValueConfig commerceValueConfig;


    @Override
    public List<String> getRecentSearches(int limit) {

        String userId = MDC.get(USER_ID_CLAIM);

        Cache cache = getRecentSearchesCache();
        Cache.ValueWrapper wrapper = cache.get(userId);
        SearchResponse<RecentSearchIndex> response = null;
        try {
            if (wrapper != null) {
                long recentSearchesBeyond = (long) wrapper.get();
                log.info("Getting recent searches for customerId={} greater than timestamp : {}", userId, recentSearchesBeyond);
                response = recentSearchElasticIndexRepository.getRecentSearches(userId,String.valueOf(recentSearchesBeyond));
            } else {
                response = recentSearchElasticIndexRepository.getRecentSearches(userId);
            }
        } catch (IOException e) {
            log.error("Exception occurred while fetching recent_searches");
            return null;
        }

        return response.hits()
                .hits()
                .stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .filter(recentSearchIndex -> recentSearchIndex.getSearchType().equalsIgnoreCase("full"))
                .collect(Collectors.toMap(
                        rs -> rs.getQuery().getNormalized(),
                        Function.identity(),
                        (existing, duplicate) -> existing,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .limit(limit)
                .map(recentSearchIndex -> recentSearchIndex.getQuery().getNormalized())
                .toList();
    }

    @Override
    public void clearRecentSearches(String userId, Date clearTime) {

        Cache cache = getRecentSearchesCache();
        log.debug("No Data found for clear recent search");
        cache.put(userId, clearTime.getTime()); // Cache the response

    }

    private Cache getRecentSearchesCache() {
        Cache cache = cacheManager.getCache(getCacheNameWithProfile(commerceValueConfig.getRedisCacheProfile(), CacheNames.CLEAR_RECENT_SEARCHES_CACHE_PREFIX));
        if (cache == null) {
            log.error("Recent Searches - Cache '{}' not found in CacheConfig", CacheNames.CLEAR_RECENT_SEARCHES_CACHE_PREFIX);
            throw new IllegalStateException("Cache not configured: " + CacheNames.CLEAR_RECENT_SEARCHES_CACHE_PREFIX);
        }
        return cache;
    }
}
