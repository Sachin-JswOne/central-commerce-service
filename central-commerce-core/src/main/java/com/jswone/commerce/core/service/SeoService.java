package com.jswone.commerce.core.service;


import com.jswone.commerce.core.model.seo.CategoryResponse;
import com.jswone.commerce.core.model.seo.SeoContext;
import com.jswone.commerce.core.model.seo.SeoData;
import com.jswone.commerce.core.model.seo.SeoMeta;

import java.util.List;
import java.util.Map;

/**
 * SeoService is the public entry point for all SEO operations.
 *
 * Responsibilities:
 * - Offline sitemap generation
 * - Runtime SEO metadata resolution
 *
 * Controllers should depend ONLY on this interface.
 */
public interface SeoService {

    /**
     * Offline sitemap generation.
     */
    List<CategoryResponse> generateSitemap();


    SeoMeta resolveSeoMeta(SeoContext seoContext, SeoData seoData);
}
