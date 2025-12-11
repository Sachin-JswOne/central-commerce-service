package com.jswone.commerce.web.controllers;

import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.service.AsyncExecutor;
import com.jswone.commerce.core.service.CacheService;
import com.jswone.commerce.core.util.ApiResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
public class CacheController implements CentralBaseController {

    private final CacheService cacheService;

    private final AsyncExecutor asyncExecutor;

    public CacheController(CacheService cacheService, AsyncExecutor asyncExecutor) {
        this.cacheService = cacheService;
        this.asyncExecutor = asyncExecutor;
    }

    @GetMapping("/buy-again/cache/keys")
    public ApiResponse<Map<String, Object>> getBuyAgainCacheKeys() {
        log.info("Fetching all buy-again cache keys");
        return cacheService.fetchBuyAgainKeys();
    }

    @DeleteMapping("/buy-again/cache/clear")
    public ApiResponse<Map<String, Object>> deleteBuyAgainCache() {
        log.info("Clearing buy-again cache");
        return cacheService.deleteBuyAgainKeys();
    }

    @PostMapping("/buy-again/cache/warmup")
    public ApiResponse<String> buyAgainProductsWarmupCache() {
        try {
            asyncExecutor.triggerBuyAgainWarmupAsync();
            return ApiResponseUtil.createSuccessResponse("Cache warm-up of buy again products for all customers started!",
                    HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error during buy-again product cache warmup", e);
            return ApiResponseUtil.createErrorResponse("Cache warm-up of buy again products for all customers failed: "
                    + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
