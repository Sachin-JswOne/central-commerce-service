package com.jswone.commerce.core.service;

import com.jswone.commerce.core.service.recent.search.RecentSearchService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;

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
    public void clearRecentSearches(String userId, Date clearTime) {
        recentSearchService.clearRecentSearches(userId, clearTime);
    }
}
