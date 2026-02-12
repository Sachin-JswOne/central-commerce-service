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

    /**
     * Resolves a slug or URL into SeoContext with entity type hint.
     * This is the preferred method as it eliminates the need for manual prefix construction.
     * 
     * @param slugOrUrl The slug or URL to resolve (e.g., "mumbai/tmt-bars" or "tmt-bars")
     * @param entityType The type of entity (PRODUCT, CATEGORY, or VARIANT)
     * @return Resolved SeoContext with entity type, location, slug, and variant MMID if applicable
     */
    SeoContext resolve(String slugOrUrl, SeoEntityType entityType);

    /**
     * Resolves a slug or URL into SeoContext (backward compatible).
     * For full URLs with prefixes like "product-detail/" or "category/".
     * 
     * @param slugOrUrl The slug or URL to resolve
     * @return Resolved SeoContext
     */
    SeoContext resolve(String slugOrUrl);
}
