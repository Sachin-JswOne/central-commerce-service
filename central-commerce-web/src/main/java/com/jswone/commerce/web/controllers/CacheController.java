package com.jswone.commerce.web.controllers;

import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.service.impl.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
public class CacheController implements CentralBaseController {

    private final CacheService cacheService;

    public CacheController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @GetMapping("/all/cache")
    public ApiResponse<Map<String, Integer>> getAllCache() {
        log.info("Request to get all redis cache");

        return ApiResponse.<Map<String, Integer>>builder().data(cacheService.getAllCache()).build();
    }

    @DeleteMapping("/buy-again/cache/clear")
    public ApiResponse<Map<String, Object>> deleteBuyAgainCache() {
        log.info("Request to clear buy-again cache");

        return cacheService.deleteBuyAgainCache();
    }
}
