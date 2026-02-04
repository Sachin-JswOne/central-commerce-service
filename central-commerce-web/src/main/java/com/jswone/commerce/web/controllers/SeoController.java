package com.jswone.commerce.web.controllers;

import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.model.seo.CategoryResponse;
import com.jswone.commerce.core.service.SeoService;
import com.jswone.commerce.web.util.ApiResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SeoController implements CentralBaseController {

    private final SeoService seoService;


    /**
     * Triggers sitemap generation.
     */
    @PostMapping("/internal/seo/v1/generate-sitemap")
    public ApiResponse<List<CategoryResponse>> generateSitemap() {
        return ApiResponseUtil.createSuccessResponse(seoService.generateSitemap(),HttpStatus.OK);
    }

}
