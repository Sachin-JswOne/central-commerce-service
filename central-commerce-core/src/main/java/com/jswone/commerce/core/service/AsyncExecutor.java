package com.jswone.commerce.core.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncExecutor {

    private final CacheService cacheService;

    public AsyncExecutor(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Async
    public void triggerBuyAgainWarmupAsync() {
        cacheService.loadAllBuyAgainProductsForCustomersIntoCache();
    }
}
