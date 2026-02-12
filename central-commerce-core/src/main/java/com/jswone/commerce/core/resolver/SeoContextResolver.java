package com.jswone.commerce.core.resolver;

import com.jswone.commerce.core.enums.seo.SeoEntityType;
import com.jswone.commerce.core.model.seo.SeoContext;

/**
 * Resolves SEO-friendly URLs or slugs into structured SeoContext objects.
 * 
 * The resolver can handle:
 * - Simple slugs: "tmt-bars"
 * - Location-based slugs: "mumbai/tmt-bars"
 * - Variant URLs: "mumbai/tmt-bars-8mm/12345678-10000001"
 * - Full SEO URLs: "product-detail/mumbai/tmt-bars"
 */
public interface SeoContextResolver {

    SeoContext resolve(String slugOrUrl, SeoEntityType entityType);

}
