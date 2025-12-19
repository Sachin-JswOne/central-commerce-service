package com.jswone.commerce.web.controllers;

import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.service.recentSearch.RecentSearchService;
import com.jswone.commerce.core.util.ApiResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RecentSearchController implements CentralBaseController{

    private final RecentSearchService recentSearchService;

    @GetMapping("/recent-search")
    public ApiResponse<List<String>> getRecentSearches() {
        return ApiResponseUtil.createSuccessResponse(recentSearchService.getRecentSearches(5), HttpStatus.OK);
    }

}
