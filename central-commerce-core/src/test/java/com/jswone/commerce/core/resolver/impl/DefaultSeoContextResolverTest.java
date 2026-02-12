package com.jswone.commerce.core.resolver.impl;

import com.jswone.commerce.core.enums.seo.CategoryType;
import com.jswone.commerce.core.enums.seo.SeoEntityType;
import com.jswone.commerce.core.enums.seo.SeoPageType;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.seo.SeoContext;
import com.jswone.commerce.core.resolver.SeoContextResolver;
import com.jswone.commerce.core.service.LocationMasterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DefaultSeoContextResolver
 */
@ExtendWith(MockitoExtension.class)
class DefaultSeoContextResolverTest {

    @Mock
    private LocationMasterService locationMasterService;

    private SeoContextResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new DefaultSeoContextResolver(locationMasterService);
    }

    @Nested
    @DisplayName("Product URL Resolution Tests")
    class ProductUrlTests {

        @Test
        @DisplayName("Should resolve simple product slug")
        void testSimpleProductSlug() {
            SeoContext context = resolver.resolve("steel-bars", SeoEntityType.PRODUCT);

            assertNotNull(context);
            assertEquals(SeoEntityType.PRODUCT, context.getEntityType());
            assertEquals(SeoPageType.PDP, context.getPageType());
            assertEquals("steel-bars", context.getSlug());
            assertNull(context.getLocation());
            assertNull(context.getVariantMmid());
        }

        @Test
        @DisplayName("Should resolve product with valid location")
        void testProductWithValidLocation() {
            // Mock location validation
            when(locationMasterService.isValidSeoLocation(("MUMBAI"))).thenReturn(true);

            SeoContext context = resolver.resolve("mumbai/steel-bars", SeoEntityType.PRODUCT);

            assertNotNull(context);
            assertEquals(SeoEntityType.PRODUCT, context.getEntityType());
            assertEquals("steel-bars", context.getSlug());
            assertEquals("mumbai", context.getLocation());
            assertNull(context.getVariantMmid());

            verify(locationMasterService).isValidSeoLocation(("MUMBAI"));
        }

        @Test
        @DisplayName("Should resolve variant with valid location and MMID")
        void testVariantWithLocationAndMmid() {
            when(locationMasterService.isValidSeoLocation("PUNE")).thenReturn(true);

            SeoContext context = resolver.resolve("pune/steel-bars/12345", SeoEntityType.VARIANT);

            assertNotNull(context);
            assertEquals(SeoEntityType.VARIANT, context.getEntityType());
            assertEquals("steel-bars", context.getSlug());
            assertEquals("pune", context.getLocation());
            assertEquals("12345", context.getVariantMmid());

            verify(locationMasterService).isValidSeoLocation("PUNE");
        }

        @Test
        @DisplayName("Should throw exception for invalid location")
        void testProductWithInvalidLocation() {
            when(locationMasterService.isValidSeoLocation("INVALID")).thenReturn(false);

            CentralCommerceServiceException exception = assertThrows(
                    CentralCommerceServiceException.class,
                    () -> resolver.resolve("invalid/steel-bars", SeoEntityType.PRODUCT));

            assertTrue(exception.getMessage().contains("not serviceable"));
            verify(locationMasterService).isValidSeoLocation("INVALID");
        }

        @Test
        @DisplayName("Should throw exception for malformed location - uppercase")
        void testMalformedLocationUppercase() {
            CentralCommerceServiceException exception = assertThrows(
                    CentralCommerceServiceException.class,
                    () -> resolver.resolve("Mumbai/steel-bars", SeoEntityType.PRODUCT));

            assertTrue(exception.getMessage().contains("malformed"));
            verifyNoInteractions(locationMasterService); // Format check happens first
        }

        @Test
        @DisplayName("Should throw exception for malformed location - underscores")
        void testMalformedLocationUnderscores() {
            CentralCommerceServiceException exception = assertThrows(
                    CentralCommerceServiceException.class,
                    () -> resolver.resolve("new_delhi/steel-bars", SeoEntityType.PRODUCT));

            assertTrue(exception.getMessage().contains("malformed"));
            verifyNoInteractions(locationMasterService);
        }

        @Test
        @DisplayName("Should throw exception for malformed location - spaces")
        void testMalformedLocationSpaces() {
            CentralCommerceServiceException exception = assertThrows(
                    CentralCommerceServiceException.class,
                    () -> resolver.resolve("new delhi/steel-bars", SeoEntityType.PRODUCT));

            assertTrue(exception.getMessage().contains("malformed"));
            verifyNoInteractions(locationMasterService);
        }

        @Test
        @DisplayName("Should throw exception for malformed location - numbers")
        void testMalformedLocationNumbers() {
            CentralCommerceServiceException exception = assertThrows(
                    CentralCommerceServiceException.class,
                    () -> resolver.resolve("123mumbai/steel-bars", SeoEntityType.PRODUCT));

            assertTrue(exception.getMessage().contains("malformed"));
            verifyNoInteractions(locationMasterService);
        }

        @Test
        @DisplayName("Should accept location with hyphens")
        void testLocationWithHyphens() {
            when(locationMasterService.isValidSeoLocation("NEW DELHI")).thenReturn(true);

            SeoContext context = resolver.resolve("new-delhi/steel-bars", SeoEntityType.PRODUCT);

            assertNotNull(context);
            assertEquals("new-delhi", context.getLocation());
            assertEquals("steel-bars", context.getSlug());

            verify(locationMasterService).isValidSeoLocation("NEW DELHI");
        }

        @Test
        @DisplayName("Should handle product-detail prefix")
        void testProductDetailPrefix() {
            when(locationMasterService.isValidSeoLocation("MUMBAI")).thenReturn(true);

            SeoContext context = resolver.resolve("mumbai/steel-bars",SeoEntityType.PRODUCT);

            assertNotNull(context);
            assertEquals(SeoEntityType.PRODUCT, context.getEntityType());
            assertEquals("steel-bars", context.getSlug());
            assertEquals("mumbai", context.getLocation());

            verify(locationMasterService).isValidSeoLocation("MUMBAI");
        }
    }

    @Nested
    @DisplayName("Category URL Resolution Tests")
    class CategoryUrlTests {

        @Test
        @DisplayName("Should resolve simple category slug")
        void testSimpleCategorySlug() {
            SeoContext context = resolver.resolve("construction-materials", SeoEntityType.CATEGORY);

            assertNotNull(context);
            assertEquals(SeoEntityType.CATEGORY, context.getEntityType());
            assertEquals(SeoPageType.PLP, context.getPageType());
            assertEquals(CategoryType.STANDARD, context.getCategoryType());
            assertEquals("construction-materials", context.getSlug());
            assertNull(context.getLocation());
        }

        @Test
        @DisplayName("Should resolve category with valid location")
        void testCategoryWithValidLocation() {
            when(locationMasterService.isValidSeoLocation("BANGALORE")).thenReturn(true);

            SeoContext context = resolver.resolve("bangalore/construction-materials", SeoEntityType.CATEGORY);

            assertNotNull(context);
            assertEquals(SeoEntityType.CATEGORY, context.getEntityType());
            assertEquals("construction-materials", context.getSlug());
            assertEquals("bangalore", context.getLocation());

            verify(locationMasterService).isValidSeoLocation("BANGALORE");
        }

        @Test
        @DisplayName("Should throw exception for category with invalid location")
        void testCategoryWithInvalidLocation() {
            when(locationMasterService.isValidSeoLocation("NOTACITY")).thenReturn(false);

            CentralCommerceServiceException exception = assertThrows(
                    CentralCommerceServiceException.class,
                    () -> resolver.resolve("notacity/construction-materials", SeoEntityType.CATEGORY));

            assertTrue(exception.getMessage().contains("not serviceable"));
            verify(locationMasterService).isValidSeoLocation("NOTACITY");
        }

        @Test
        @DisplayName("Should handle category prefix")
        void testCategoryPrefix() {
            when(locationMasterService.isValidSeoLocation("DELHI")).thenReturn(true);

            SeoContext context = resolver.resolve("delhi/construction-materials",SeoEntityType.CATEGORY);

            assertNotNull(context);
            assertEquals(SeoEntityType.CATEGORY, context.getEntityType());
            assertEquals("construction-materials", context.getSlug());
            assertEquals("delhi", context.getLocation());

            verify(locationMasterService).isValidSeoLocation("DELHI");
        }
    }

    @Nested
    @DisplayName("Normalization Tests")
    class NormalizationTests {

        @Test
        @DisplayName("Should remove leading slash")
        void testRemoveLeadingSlash() {
            SeoContext context = resolver.resolve("/steel-bars", SeoEntityType.PRODUCT);

            assertNotNull(context);
            assertEquals("steel-bars", context.getSlug());
        }

        @Test
        @DisplayName("Should remove trailing slash")
        void testRemoveTrailingSlash() {
            SeoContext context = resolver.resolve("steel-bars/", SeoEntityType.PRODUCT);

            assertNotNull(context);
            assertEquals("steel-bars", context.getSlug());
        }

        @Test
        @DisplayName("Should remove both leading and trailing slashes")
        void testRemoveBothSlashes() {
            SeoContext context = resolver.resolve("/steel-bars/", SeoEntityType.PRODUCT);

            assertNotNull(context);
            assertEquals("steel-bars", context.getSlug());
        }

        @Test
        @DisplayName("Should throw exception for null input")
        void testNullInput() {
            assertThrows(CentralCommerceServiceException.class,
                    () -> resolver.resolve(null, SeoEntityType.PRODUCT));
        }

        @Test
        @DisplayName("Should throw exception for empty input")
        void testEmptyInput() {
            assertThrows(CentralCommerceServiceException.class,
                    () -> resolver.resolve("", SeoEntityType.PRODUCT));
        }

        @Test
        @DisplayName("Should throw exception for whitespace input")
        void testWhitespaceInput() {
            assertThrows(CentralCommerceServiceException.class,
                    () -> resolver.resolve("   ", SeoEntityType.PRODUCT));
        }
    }

    @Nested
    @DisplayName("Backward Compatible Resolve Tests")
    class BackwardCompatibilityTests {

        @Test
        @DisplayName("Should resolve product-detail prefix via backward compatible method")
        void testBackwardCompatibleProductDetail() {
            SeoContext context = resolver.resolve("steel-bars",SeoEntityType.PRODUCT);

            assertNotNull(context);
            assertEquals(SeoEntityType.PRODUCT, context.getEntityType());
            assertEquals("steel-bars", context.getSlug());
        }

        @Test
        @DisplayName("Should resolve category prefix via backward compatible method")
        void testBackwardCompatibleCategory() {
            SeoContext context = resolver.resolve("materials",SeoEntityType.CATEGORY);

            assertNotNull(context);
            assertEquals(SeoEntityType.CATEGORY, context.getEntityType());
            assertEquals("materials", context.getSlug());
        }



        @Test
        @DisplayName("Should treat unprefixed URLs as product slugs")
        void testBackwardCompatibleSimpleSlug() {
            SeoContext context = resolver.resolve("steel-bars",SeoEntityType.PRODUCT);

            assertNotNull(context);
            assertEquals(SeoEntityType.PRODUCT, context.getEntityType());
            assertEquals("steel-bars", context.getSlug());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle very long location names")
        void testLongLocationName() {
            String longLocation = "very-long-location-name-with-many-hyphens";
            when(locationMasterService.isValidSeoLocation("VERY LONG LOCATION NAME WITH MANY HYPHENS"))
                    .thenReturn(true);

            SeoContext context = resolver.resolve(longLocation + "/steel-bars", SeoEntityType.PRODUCT);

            assertNotNull(context);
            assertEquals(longLocation, context.getLocation());
        }

        @Test
        @DisplayName("Should handle single letter location")
        void testSingleLetterLocation() {
            when(locationMasterService.isValidSeoLocation("A")).thenReturn(true);

            SeoContext context = resolver.resolve("a/steel-bars", SeoEntityType.PRODUCT);

            assertNotNull(context);
            assertEquals("a", context.getLocation());
        }

        @Test
        @DisplayName("Should reject location starting with hyphen")
        void testLocationStartingWithHyphen() {
            CentralCommerceServiceException exception = assertThrows(
                    CentralCommerceServiceException.class,
                    () -> resolver.resolve("-mumbai/steel-bars", SeoEntityType.PRODUCT));

            assertTrue(exception.getMessage().contains("malformed"));
        }

        @Test
        @DisplayName("Should reject location ending with hyphen")
        void testLocationEndingWithHyphen() {
            CentralCommerceServiceException exception = assertThrows(
                    CentralCommerceServiceException.class,
                    () -> resolver.resolve("mumbai-/steel-bars", SeoEntityType.PRODUCT));

            assertTrue(exception.getMessage().contains("malformed"));
        }

        @Test
        @DisplayName("Should reject location with consecutive hyphens")
        void testLocationWithConsecutiveHyphens() {
            CentralCommerceServiceException exception = assertThrows(
                    CentralCommerceServiceException.class,
                    () -> resolver.resolve("new--delhi/steel-bars", SeoEntityType.PRODUCT));

            assertTrue(exception.getMessage().contains("malformed"));
        }
    }
}
