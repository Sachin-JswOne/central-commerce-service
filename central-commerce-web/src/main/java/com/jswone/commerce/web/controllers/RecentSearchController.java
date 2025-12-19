package com.jswone.commerce.web.controllers;

import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.service.AsyncExecutor;
import com.jswone.commerce.core.service.recentSearch.RecentSearchService;
import com.jswone.commerce.core.util.ApiResponseUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.jswone.commerce.core.constants.JWTConstants.USER_ID_CLAIM;

@RestController
@RequiredArgsConstructor
public class RecentSearchController implements CentralBaseController {

    private final RecentSearchService recentSearchService;
    private final AsyncExecutor asyncExecutor;

    @GetMapping("/recent-search")
    public ApiResponse<List<String>> getRecentSearches() {
        return ApiResponseUtil.createSuccessResponse(recentSearchService.getRecentSearches(5), HttpStatus.OK);
    }

    @DeleteMapping("/recent-search")
    public ApiResponse<String> clearRecentSearches() {
        String userId = MDC.get(USER_ID_CLAIM);
        asyncExecutor.clearRecentSearches(userId);

        return ApiResponseUtil.createSuccessResponse("Clear recent search initiated", HttpStatus.OK);
    }
}
