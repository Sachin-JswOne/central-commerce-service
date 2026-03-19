package com.jswone.commerce.web.controllers;

import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.service.trendingSearch.TrendingSearchService;
import com.jswone.commerce.core.util.ApiResponseUtil;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class TrendingSearchController {

    @Value("${recent.search.limit}")
    private int recentSearchLimit;

    private final TrendingSearchService trendingSearchService;

    @PostMapping("/admin/bar-terms")
    public ApiResponse<String> barTerms(@RequestBody @NotEmpty(message = "bar Terms cannot be empty") Set<String> barTerms) {
        trendingSearchService.barTerms(barTerms);
        return ApiResponseUtil.createSuccessResponse("Terms Barred Successfully", HttpStatus.OK);
    }

    @GetMapping("/trending-search")
    public ApiResponse<List<String>> getTrendingSearches() {
        return ApiResponseUtil.createSuccessResponse(trendingSearchService.getTrendingSearchTerms(recentSearchLimit, "30"), HttpStatus.OK);
    }

}
