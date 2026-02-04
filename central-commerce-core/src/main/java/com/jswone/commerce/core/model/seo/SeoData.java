package com.jswone.commerce.core.model.seo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Data fetched from Central Catalogue for SEO metadata generation
 */
@Getter
@Builder
@AllArgsConstructor
public class SeoData {
    private final String title; // Product title or Category title
    private final String image; // Product/Category/Brand image for og:image
    private final Map<String, String> defaultSelectedAttributes; // Default variant attributes
}
