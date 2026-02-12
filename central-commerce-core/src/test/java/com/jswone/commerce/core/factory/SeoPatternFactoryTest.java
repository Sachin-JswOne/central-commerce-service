package com.jswone.commerce.core.factory;

import com.jswone.commerce.core.enums.seo.SeoEntityType;
import com.jswone.commerce.core.enums.seo.SeoPageType;
import com.jswone.commerce.core.model.seo.SeoContext;
import com.jswone.commerce.core.pattern.SeoPatternHandler;
import com.jswone.commerce.core.pattern.impl.DefaultSeoPatternHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test suite for SeoPatternFactory
 * Verifies correct handler selection based on SeoContext
 */
@ExtendWith(MockitoExtension.class)
class SeoPatternFactoryTest {

    @Mock
    private DefaultSeoPatternHandler mockDefaultHandler;

    private SeoPatternFactory patternFactory;

    @BeforeEach
    void setUp() {
        patternFactory = new SeoPatternFactory(mockDefaultHandler);
    }

    @Test
    void resolve_shouldReturnHandler_whenProductEntityType() {
        // Given
        SeoContext productContext = SeoContext.builder()
                .slug("tmt-bars")
                .entityType(SeoEntityType.PRODUCT)
                .pageType(SeoPageType.PDP)
                .build();

        // When
        SeoPatternHandler handler = patternFactory.resolve(productContext);

        // Then
        assertNotNull(handler);
        assertEquals(mockDefaultHandler, handler);
    }

    @Test
    void resolve_shouldReturnHandler_whenVariantEntityType() {
        // Given
        SeoContext variantContext = SeoContext.builder()
                .slug("tmt-bars-8mm")
                .variantMmid("12345678-10000001")
                .entityType(SeoEntityType.VARIANT)
                .pageType(SeoPageType.PDP)
                .build();

        // When
        SeoPatternHandler handler = patternFactory.resolve(variantContext);

        // Then
        assertNotNull(handler);
        assertEquals(mockDefaultHandler, handler);
    }

    @Test
    void resolve_shouldReturnCategoryHandler_whenCategoryEntityType() {
        // Given
        SeoContext categoryContext = SeoContext.builder()
                .slug("steel-products")
                .entityType(SeoEntityType.CATEGORY)
                .pageType(SeoPageType.PLP)
                .build();

        // When
        SeoPatternHandler handler = patternFactory.resolve(categoryContext);

        // Then
        assertNotNull(handler);
        assertEquals(mockDefaultHandler, handler);
    }

    @Test
    void resolve_shouldHandleNullEntityType() {
        // Given
        SeoContext nullEntityContext = SeoContext.builder()
                .slug("test-product")
                .pageType(SeoPageType.PDP)
                .build();

        // When & Then
        // Verify appropriate default handling or exception
        assertDoesNotThrow(() -> patternFactory.resolve(nullEntityContext));
    }

    @Test
    void resolve_shouldReturnSameHandler_forSimilarContexts() {
        // Given
        SeoContext context1 = SeoContext.builder()
                .slug("product-1")
                .entityType(SeoEntityType.PRODUCT)
                .pageType(SeoPageType.PDP)
                .build();

        SeoContext context2 = SeoContext.builder()
                .slug("product-2")
                .entityType(SeoEntityType.PRODUCT)
                .pageType(SeoPageType.PDP)
                .build();

        // When
        SeoPatternHandler handler1 = patternFactory.resolve(context1);
        SeoPatternHandler handler2 = patternFactory.resolve(context2);

        // Then
        assertNotNull(handler1);
        assertNotNull(handler2);
        // Both should return the same mock handler instance
        assertEquals(handler1, handler2);
        assertEquals(mockDefaultHandler, handler1);
    }

    @Test
    void resolve_shouldHandleLocationSpecificContext() {
        // Given
        SeoContext locationContext = SeoContext.builder()
                .slug("tmt-bars")
                .location("mumbai")
                .entityType(SeoEntityType.PRODUCT)
                .pageType(SeoPageType.PDP)
                .build();

        // When
        SeoPatternHandler handler = patternFactory.resolve(locationContext);

        // Then
        assertNotNull(handler);
        assertEquals(mockDefaultHandler, handler);
        // Location should not affect handler selection, only metadata generation
    }
}
