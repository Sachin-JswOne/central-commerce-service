package com.jswone.commerce.core.resolver.impl;

import com.jswone.commerce.core.enums.seo.SeoEntityType;
import com.jswone.commerce.core.enums.seo.SeoPageType;
import com.jswone.commerce.core.model.seo.SeoContext;
import com.jswone.commerce.core.resolver.SeoContextResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DefaultSeoContextResolver.resolve() method
 */
class DefaultSeoContextResolverTest {

    private SeoContextResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new DefaultSeoContextResolver();
    }

    @Test
    void resolve_shouldReturnProductContext_whenSimpleSlugProvided() {
        // Given
        String simpleSlug = "steel-pipe";

        // When
        SeoContext result = resolver.resolve(simpleSlug);

        // Then
        assertNotNull(result);
        assertEquals("steel-pipe", result.getSlug());
        assertEquals(SeoEntityType.PRODUCT, result.getEntityType());
        assertEquals(SeoPageType.PDP, result.getPageType());
        assertNull(result.getLocation());
        assertNull(result.getVariantMmid());
    }

    @Test
    void resolve_shouldExtractSlugAndContext_whenProductDetailBaseUrl() {
        // Given
        String seoUrl = "/product-detail/steel-pipe";

        // When
        SeoContext result = resolver.resolve(seoUrl);

        // Then
        assertNotNull(result);
        assertEquals("steel-pipe", result.getSlug());
        assertEquals(SeoEntityType.PRODUCT, result.getEntityType());
        assertEquals(SeoPageType.PDP, result.getPageType());
        assertNull(result.getLocation());
        assertNull(result.getVariantMmid());
    }

    @Test
    void resolve_shouldExtractSlugAndLocation_whenProductDetailWithLocation() {
        // Given
        String seoUrl = "/product-detail/mumbai/steel-pipe";

        // When
        SeoContext result = resolver.resolve(seoUrl);

        // Then
        assertNotNull(result);
        assertEquals("steel-pipe", result.getSlug());
        assertEquals("mumbai", result.getLocation());
        assertEquals(SeoEntityType.PRODUCT, result.getEntityType());
        assertEquals(SeoPageType.PDP, result.getPageType());
        assertNull(result.getVariantMmid());
    }

    @Test
    void resolve_shouldExtractAllComponents_whenVariantUrlWithLocationAndMmid() {
        // Given
        String seoUrl = "/product-detail/mumbai/steel-pipe-thickness-10mm/12345678-10000001";

        // When
        SeoContext result = resolver.resolve(seoUrl);

        // Then
        assertNotNull(result);
        assertEquals("steel-pipe-thickness-10mm", result.getSlug());
        assertEquals("mumbai", result.getLocation());
        assertEquals("12345678-10000001", result.getVariantMmid());
        assertEquals(SeoEntityType.VARIANT, result.getEntityType());
        assertEquals(SeoPageType.PDP, result.getPageType());
    }

    @Test
    void resolve_shouldExtractSlug_whenNoLeadingSlash() {
        // Given
        String seoUrl = "product-detail/mumbai/steel-pipe";

        // When
        SeoContext result = resolver.resolve(seoUrl);

        // Then
        assertNotNull(result);
        assertEquals("steel-pipe", result.getSlug());
        assertEquals("mumbai", result.getLocation());
    }

    @Test
    void resolve_shouldExtractSlug_whenTrailingSlash() {
        // Given
        String seoUrl = "/product-detail/mumbai/steel-pipe/";

        // When
        SeoContext result = resolver.resolve(seoUrl);

        // Then
        assertNotNull(result);
        assertEquals("steel-pipe", result.getSlug());
        assertEquals("mumbai", result.getLocation());
    }

    @Test
    void resolve_shouldThrowException_whenNull() {
        // When & Then
        assertThrows(Exception.class, () -> resolver.resolve(null));
    }

    @Test
    void resolve_shouldThrowException_whenEmptyString() {
        // When & Then
        assertThrows(Exception.class, () -> resolver.resolve(""));
    }

    @Test
    void resolve_shouldHandleWhitespace() {
        // Given
        String seoUrl = "  /product-detail/delhi/steel-beam  ";

        // When
        SeoContext result = resolver.resolve(seoUrl);

        // Then
        assertNotNull(result);
        assertEquals("steel-beam", result.getSlug());
        assertEquals("delhi", result.getLocation());
    }

    @Test
    void resolve_shouldExtractComplexSlug_withMultipleHyphens() {
        // Given
        String seoUrl = "/product-detail/bangalore/jsw-neo-steel-tmt-bars-fe-550d";

        // When
        SeoContext result = resolver.resolve(seoUrl);

        // Then
        assertNotNull(result);
        assertEquals("jsw-neo-steel-tmt-bars-fe-550d", result.getSlug());
        assertEquals("bangalore", result.getLocation());
    }

    @Test
    void resolve_shouldHandleSlugWithNumbers() {
        // Given
        String seoUrl = "/product-detail/chennai/ms-pipe-50mm-grade-a106";

        // When
        SeoContext result = resolver.resolve(seoUrl);

        // Then
        assertNotNull(result);
        assertEquals("ms-pipe-50mm-grade-a106", result.getSlug());
        assertEquals("chennai", result.getLocation());
    }

    @Test
    void resolve_shouldExtractSlug_whenOnlyProductDetailPrefix() {
        // Given
        String seoUrl = "product-detail/steel-angle";

        // When
        SeoContext result = resolver.resolve(seoUrl);

        // Then
        assertNotNull(result);
        assertEquals("steel-angle", result.getSlug());
        assertNull(result.getLocation());
    }
}
