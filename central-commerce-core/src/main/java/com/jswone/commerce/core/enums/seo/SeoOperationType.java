package com.jswone.commerce.core.enums.seo;

/**
 * Represents the type of SEO operation being performed.
 */
public enum SeoOperationType {
    /**
     * Generating URLs for sitemap (offline process)
     */
    URL_GENERATION,

    /**
     * Generating metadata for runtime resolution (online process)
     */
    METADATA_RESOLUTION
}
