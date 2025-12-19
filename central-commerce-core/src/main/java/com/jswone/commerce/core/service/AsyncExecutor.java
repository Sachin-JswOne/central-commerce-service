package com.jswone.commerce.core.service;

import com.jswone.commerce.core.service.recentSearch.RecentSearchService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncExecutor {

    private final CacheService cacheService;
    private final RecentSearchService recentSearchService;

    public AsyncExecutor(CacheService cacheService, RecentSearchService recentSearchService) {
        this.cacheService = cacheService;
        this.recentSearchService = recentSearchService;
    }

    @Async
    public void triggerBuyAgainWarmupAsync() {
        cacheService.loadAllBuyAgainProductsForCustomersIntoCache();
    }

    @Async
    public void clearRecentSearches(String userId) {
        recentSearchService.clearRecentSearches(userId);
    }
}
