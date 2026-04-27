package com.jswone.commerce.web.controllers;

import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.service.AsyncExecutor;
import com.jswone.commerce.core.service.recent.search.RecentSearchService;
import com.jswone.commerce.core.util.ApiResponseUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

import static com.jswone.commerce.core.constants.JWTConstants.USER_ID_CLAIM;

@RestController
@RequiredArgsConstructor
public class RecentSearchController {

    private final RecentSearchService recentSearchService;
    private final AsyncExecutor asyncExecutor;

    @Value("${recent.search.limit}")
    private int recentSearchLimit;

    @GetMapping("/recent-search")
    public ApiResponse<List<String>> getRecentSearches() {
        return ApiResponseUtil.createSuccessResponse(recentSearchService.getRecentSearches(recentSearchLimit), HttpStatus.OK);
    }

    @DeleteMapping("/recent-search")
    public ApiResponse<String> clearRecentSearches() {
        String userId = MDC.get(USER_ID_CLAIM);
        Date d = new Date();
        asyncExecutor.clearRecentSearches(userId, new Date());

        return ApiResponseUtil.createSuccessResponse("Clear recent search initiated at : ".concat(d.toString()), HttpStatus.OK);
    }
}
