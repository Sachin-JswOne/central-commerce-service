package com.jswone.commerce.core.resolver.impl;

import com.jswone.commerce.core.enums.seo.SeoEntityType;
import com.jswone.commerce.core.enums.seo.SeoOperationType;
import com.jswone.commerce.core.enums.seo.SeoPageType;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.seo.SeoContext;
import com.jswone.commerce.core.resolver.SeoContextResolver;
import com.jswone.commerce.core.service.LocationMasterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultSeoContextResolver implements SeoContextResolver {

    private final LocationMasterService locationMasterService;

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

    /**
     * Validate location format and existence: must be lowercase letters with
     * hyphens only,
     * and must exist in the location master data.
     * Valid examples: "mumbai", "new-delhi", "andhra-pradesh"
     * Invalid examples: "Mumbai", "new_delhi", "123delhi", "new delhi"
     *
     */
    private void validateLocationFormat(String location) {
        if (location == null || location.isEmpty()) {
            return; // null/empty is valid (no location specified)
        }

        // Pattern: lowercase letters and hyphens only, must start and end with letter
        if (!location.matches("^[a-z]+(-[a-z]+)*$")) {
            log.error("Malformed location in URL: {}", location);
            throw new CentralCommerceServiceException(
                    "The requested location in URL is malformed",
                    org.springframework.http.HttpStatus.NOT_FOUND);
        }

        String normalizedLocation = formatSeoLocationNameToUpperCase(location);

        // Validate against location master data
        if (!locationMasterService.isValidSeoLocation(normalizedLocation)) {
            log.warn("Location '{}' (normalized: '{}') not found in serviceable locations",
                    location, normalizedLocation);
            throw new CentralCommerceServiceException(
                    "The requested location is not serviceable",
                    org.springframework.http.HttpStatus.NOT_FOUND);
        }

        log.debug("Location '{}' validated successfully", location);
    }

    /**
     * Convert SEO URL location format (lowercase-with-hyphens) to uppercase format.
     * Example: "new-delhi" -> "NEW DELHI"
     */
    private String formatSeoLocationNameToUpperCase(String seoLocation) {
        return seoLocation.replace("-", " ").toUpperCase();
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

        // Validate location format if present
        validateLocationFormat(location);

        return SeoContext.builder()
                .entityType(SeoEntityType.CATEGORY)
                .pageType(SeoPageType.PLP)
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

        // Validate location format if present
        validateLocationFormat(location);

        SeoEntityType entityType = mmid != null ? SeoEntityType.VARIANT : SeoEntityType.PRODUCT;

        return SeoContext.builder()
                .entityType(entityType)
                .pageType(SeoPageType.PDP)
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
                .slug(slug)
                .operationType(SeoOperationType.METADATA_RESOLUTION)
                .build();
    }
}