package com.jswone.commerce.core.resolver.impl;

import com.jswone.commerce.core.enums.seo.CategoryType;
import com.jswone.commerce.core.enums.seo.SeoEntityType;
import com.jswone.commerce.core.enums.seo.SeoOperationType;
import com.jswone.commerce.core.enums.seo.SeoPageType;
import com.jswone.commerce.core.model.seo.SeoContext;
import com.jswone.commerce.core.resolver.SeoContextResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Default implementation of SeoContextResolver.
 *
 * Converts SEO-friendly URLs into SeoContext.
 */
@Slf4j
@Component
public class DefaultSeoContextResolver implements SeoContextResolver {

    @Override
    public SeoContext resolve(String path, Map<String, String> vars) {

        String normalizedPath = path.startsWith("/") ? path : "/" + path;

        if (normalizedPath.startsWith("/brand")) {
            return resolveBrand(vars);
        }

        if (normalizedPath.startsWith("/category")) {
            return resolveCategory(vars);
        }

        if (normalizedPath.startsWith("/product-detail")) {
            return resolveProduct(vars);
        }

        throw new IllegalArgumentException("Unsupported SEO URL: " + path);
    }

    /*
     * =========================================================
     * BRAND
     * /brand/{slug}
     * /brand/{location}/{slug}
     * =========================================================
     */
    private SeoContext resolveBrand(Map<String, String> vars) {

        return SeoContext.builder()
                .entityType(SeoEntityType.CATEGORY)
                .pageType(SeoPageType.PLP)
                .categoryType(CategoryType.BRAND)
                .slug(vars.get("slug"))
                .location(vars.get("location"))
                .operationType(SeoOperationType.METADATA_RESOLUTION)
                .build();
    }

    /*
     * =========================================================
     * CATEGORY
     * /category/{slug}
     * /category/{location}/{slug}
     * =========================================================
     */
    private SeoContext resolveCategory(Map<String, String> vars) {

        return SeoContext.builder()
                .entityType(SeoEntityType.CATEGORY)
                .pageType(SeoPageType.PLP)
                .categoryType(CategoryType.STANDARD)
                .slug(vars.get("slug"))
                .location(vars.get("location"))
                .operationType(SeoOperationType.METADATA_RESOLUTION)
                .build();
    }

    /*
     * =========================================================
     * PRODUCT / VARIANT
     * /product-detail/{slug}
     * /product-detail/{location}/{slug}
     * /product-detail/{location}/{slug}/{mmid}
     * =========================================================
     */
    private SeoContext resolveProduct(Map<String, String> vars) {

        String mmid = vars.get("mmid");

        SeoEntityType entityType = mmid != null ? SeoEntityType.VARIANT : SeoEntityType.PRODUCT;

        return SeoContext.builder()
                .entityType(entityType)
                .pageType(SeoPageType.PDP)
                .categoryType(CategoryType.STANDARD)
                .slug(vars.get("slug"))
                .location(vars.get("location"))
                .variantMmid(mmid)
                .operationType(SeoOperationType.METADATA_RESOLUTION)
                .build();
    }
}