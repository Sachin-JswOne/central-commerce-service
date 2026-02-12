package com.jswone.commerce.core.resolver.impl;

import com.jswone.commerce.core.enums.seo.CategoryType;
import com.jswone.commerce.core.enums.seo.SeoEntityType;
import com.jswone.commerce.core.enums.seo.SeoOperationType;
import com.jswone.commerce.core.enums.seo.SeoPageType;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.seo.SeoContext;
import com.jswone.commerce.core.resolver.SeoContextResolver;
import com.jswone.commerce.core.constants.SeoConstants;
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

    /**
     * Resolves with entity type hint - cleaner approach without prefix
     * construction.
     */
    @Override
    public SeoContext resolve(String slugOrUrl, SeoEntityType entityType) {
        String normalized = normalize(slugOrUrl);

        log.debug("Resolving slug '{}' with entity type: {}", normalized, entityType);

        // Handle based on entity type
        // Note: BRAND is handled as CATEGORY type
        if (entityType == SeoEntityType.PRODUCT || entityType == SeoEntityType.VARIANT) {
            return resolveProductUrl(normalized, true);
        } else if (entityType == SeoEntityType.CATEGORY) {
            return resolveCategoryUrl(normalized, true);
        } else {
            return resolveSimpleSlug(normalized);
        }
    }

    /**
     * Backward compatible resolve - uses prefix detection.
     */
    @Override
    public SeoContext resolve(String slugOrUrl) {

        String normalized = normalize(slugOrUrl);

        if (normalized.startsWith("brand/")) {
            return resolveBrandUrl(normalized, false);
        }

        if (normalized.startsWith("category/")) {
            return resolveCategoryUrl(normalized, false);
        }

        if (normalized.startsWith("product-detail/")) {
            return resolveProductUrl(normalized, false);
        }

        // If no prefix → treat as simple product slug
        return resolveSimpleSlug(normalized);
    }

    private String normalize(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new CentralCommerceServiceException("Input cannot be null or empty");
        }

        String normalized = input.trim();

        // Remove leading slash
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        // Remove trailing slash
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    /*
     * BRAND URL
     * brand/{slug}
     * brand/{location}/{slug}
     * OR (prefixless): {slug} or {location}/{slug}
     */
    private SeoContext resolveBrandUrl(String normalized, boolean prefixless) {
        String[] parts = normalized.split("/");
        int startIndex = prefixless ? 0 : 1; // Skip "brand/" if present

        String slug = null;
        String location = null;

        if (parts.length == startIndex + 1) {
            // {slug}
            slug = parts[startIndex];
        } else if (parts.length >= startIndex + 2) {
            // {location}/{slug}
            location = parts[startIndex];
            slug = parts[startIndex + 1];
        }

        return SeoContext.builder()
                .entityType(SeoEntityType.CATEGORY)
                .pageType(SeoPageType.PLP)
                .categoryType(CategoryType.BRAND)
                .slug(slug)
                .location(location)
                .operationType(SeoOperationType.METADATA_RESOLUTION)
                .build();
    }

    /*
     * CATEGORY URL
     * category/{slug}
     * category/{location}/{slug}
     * OR (prefixless): {slug} or {location}/{slug}
     */
    private SeoContext resolveCategoryUrl(String normalized, boolean prefixless) {
        String[] parts = normalized.split("/");
        int startIndex = prefixless ? 0 : 1; // Skip "category/" if present

        String slug = null;
        String location = null;

        if (parts.length == startIndex + 1) {
            // {slug}
            slug = parts[startIndex];
        } else if (parts.length >= startIndex + 2) {
            // {location}/{slug}
            location = parts[startIndex];
            slug = parts[startIndex + 1];
        }

        return SeoContext.builder()
                .entityType(SeoEntityType.CATEGORY)
                .pageType(SeoPageType.PLP)
                .categoryType(CategoryType.STANDARD)
                .slug(slug)
                .location(location)
                .operationType(SeoOperationType.METADATA_RESOLUTION)
                .build();
    }

    /*
     * PRODUCT / VARIANT URL
     * product-detail/{slug}
     * product-detail/{location}/{slug}
     * product-detail/{location}/{slug}/{mmid}
     * OR (prefixless): {slug}, {location}/{slug}, {location}/{slug}/{mmid}
     */
    private SeoContext resolveProductUrl(String normalized, boolean prefixless) {
        String[] parts = normalized.split("/");
        int startIndex = prefixless ? 0 : 1; // Skip "product-detail/" if present

        String slug = null;
        String location = null;
        String mmid = null;

        int partsCount = parts.length - startIndex;

        if (partsCount == 1) {
            // {slug}
            slug = parts[startIndex];
        } else if (partsCount == 2) {
            // {location}/{slug}
            location = parts[startIndex];
            slug = parts[startIndex + 1];
        } else if (partsCount >= 3) {
            // {location}/{slug}/{mmid}
            location = parts[startIndex];
            slug = parts[startIndex + 1];
            mmid = parts[startIndex + 2];
        }

        SeoEntityType entityType = mmid != null ? SeoEntityType.VARIANT : SeoEntityType.PRODUCT;

        return SeoContext.builder()
                .entityType(entityType)
                .pageType(SeoPageType.PDP)
                .categoryType(CategoryType.STANDARD)
                .slug(slug)
                .location(location)
                .variantMmid(mmid)
                .operationType(SeoOperationType.METADATA_RESOLUTION)
                .build();
    }

    /*
     * SIMPLE SLUG (no URL prefix)
     * Assumes it's a product slug
     */
    private SeoContext resolveSimpleSlug(String slug) {
        return SeoContext.builder()
                .entityType(SeoEntityType.PRODUCT)
                .pageType(SeoPageType.PDP)
                .categoryType(CategoryType.STANDARD)
                .slug(slug)
                .operationType(SeoOperationType.METADATA_RESOLUTION)
                .build();
    }
}