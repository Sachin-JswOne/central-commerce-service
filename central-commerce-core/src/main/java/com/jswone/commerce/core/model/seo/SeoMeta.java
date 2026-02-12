package com.jswone.commerce.core.model.seo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class SeoMeta {

    /* Standard SEO */
    private final String title;
    private final String description;
    private final String canonical;

    /* Open Graph */
    private final String ogTitle;
    private final String ogType;
    private final String ogUrl;
    private final String ogImage;
    private final String ogDescription;
}
