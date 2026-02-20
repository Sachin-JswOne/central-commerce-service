package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.enums.seo.SeoEntityType;
import com.jswone.commerce.core.enums.seo.SeoPageType;

import com.jswone.commerce.core.constants.SeoConstants;
import com.jswone.commerce.core.factory.SeoPatternFactory;
import com.jswone.commerce.core.model.seo.SeoContext;
import com.jswone.commerce.core.model.seo.SeoData;
import com.jswone.commerce.core.model.seo.SeoMeta;
import com.jswone.commerce.core.model.seo.UrlMeta;
import com.jswone.commerce.core.pattern.SeoPatternHandler;
import com.jswone.commerce.core.resolver.SeoContextResolver;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.service.GcsService;
import com.jswone.commerce.core.service.ProductTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.jswone.commerce.core.service.LocationMasterService;
import com.jswone.commerce.core.config.CommerceValueConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for DefaultSeoService
 * Tests the refactored resolveSeoMeta(SeoContext, SeoData) method
 */
@ExtendWith(MockitoExtension.class)
class DefaultSeoServiceTest {

        @Mock
        private SeoPatternFactory patternFactory;

        @Mock
        private SeoContextResolver contextResolver;

        @Mock
        private CentralCatalogueClient centralCatalogueClient;

        @Mock
        private ProductTypeService productTypeService;

        @Mock
        private GcsService gcsService;

        @Mock
        private LocationMasterService locationMasterService;

        @Mock
        private CommerceValueConfig commerceValueConfig;

        @Mock
        private SeoPatternHandler mockHandler;

        private DefaultSeoService seoService;

        @BeforeEach
        void setUp() {
                seoService = new DefaultSeoService(
                                patternFactory,
                                contextResolver,
                                centralCatalogueClient,
                                productTypeService,
                                locationMasterService,
                                commerceValueConfig,
                                gcsService);
        }

        // ==================== resolveSeoMeta() Tests ====================

        @Test
        void resolveSeoMeta_shouldReturnSeoMeta_whenValidInputProvided() {
                // Given
                SeoContext seoContext = SeoContext.builder()
                                .slug("tmt-bars")
                                .entityType(SeoEntityType.PRODUCT)
                                .pageType(SeoPageType.PDP)
                                .build();

                SeoData seoData = SeoData.builder()
                                .title("TMT Bars")
                                .image("https://example.com/tmt-bars.jpg")
                                .build();

                SeoMeta expectedMeta = new SeoMeta(
                                "TMT Bars - JSW One",
                                "High quality TMT bars",
                                "https://jswone.com/product-detail/tmt-bars",
                                "TMT Bars - JSW One",
                                "product",
                                "https://jswone.com/product-detail/tmt-bars",
                                "https://example.com/tmt-bars.jpg",
                                "High quality TMT bars");

                when(patternFactory.resolve(seoContext)).thenReturn(mockHandler);
                when(mockHandler.generateMeta(seoContext, seoData)).thenReturn(expectedMeta);

                // When
                SeoMeta result = seoService.resolveSeoMeta(seoContext, seoData);

                // Then
                assertNotNull(result);
                assertEquals("TMT Bars - JSW One", result.getTitle());
                assertEquals("High quality TMT bars", result.getDescription());
                assertEquals("https://jswone.com/product-detail/tmt-bars", result.getCanonical());
                assertEquals("product", result.getOgType());
                verify(patternFactory, times(1)).resolve(seoContext);
                verify(mockHandler, times(1)).generateMeta(seoContext, seoData);
        }

        @Test
        void resolveSeoMeta_shouldSelectCorrectHandler_forProductContext() {
                // Given
                SeoContext productContext = SeoContext.builder()
                                .slug("steel-pipe")
                                .entityType(SeoEntityType.PRODUCT)
                                .pageType(SeoPageType.PDP)
                                .build();

                SeoData seoData = SeoData.builder()
                                .title("Steel Pipe")
                                .build();

                SeoMeta mockMeta = new SeoMeta(
                                "Steel Pipe", "Description", "url", "og", "product", "og-url", "img", "og-desc");

                when(patternFactory.resolve(productContext)).thenReturn(mockHandler);
                when(mockHandler.generateMeta(any(SeoContext.class), any(SeoData.class))).thenReturn(mockMeta);

                // When
                seoService.resolveSeoMeta(productContext, seoData);

                // Then
                verify(patternFactory).resolve(productContext);
                verify(mockHandler).generateMeta(productContext, seoData);
        }

        @Test
        void resolveSeoMeta_shouldHandleLocationSpecificContext() {
                // Given
                SeoContext contextWithLocation = SeoContext.builder()
                                .slug("tmt-bars")
                                .location("mumbai")
                                .entityType(SeoEntityType.PRODUCT)
                                .pageType(SeoPageType.PDP)
                                .build();

                SeoData seoData = SeoData.builder()
                                .title("TMT Bars")
                                .build();

                SeoMeta locationMeta = new SeoMeta(
                                "TMT Bars in Mumbai",
                                "TMT Bars in Mumbai",
                                "https://jswone.com/product-detail/mumbai/tmt-bars",
                                "TMT Bars in Mumbai",
                                "product",
                                "https://jswone.com/product-detail/mumbai/tmt-bars",
                                "img.jpg",
                                "TMT Bars in Mumbai");

                when(patternFactory.resolve(contextWithLocation)).thenReturn(mockHandler);
                when(mockHandler.generateMeta(contextWithLocation, seoData)).thenReturn(locationMeta);

                // When
                SeoMeta result = seoService.resolveSeoMeta(contextWithLocation, seoData);

                // Then
                assertNotNull(result);
                assertTrue(result.getTitle().contains("Mumbai"));
                assertTrue(result.getCanonical().contains("mumbai"));
        }

        @Test
        void resolveSeoMeta_shouldHandleVariantContext() {
                // Given
                SeoContext variantContext = SeoContext.builder()
                                .slug("tmt-bars-8mm")
                                .variantMmid("12345678-10000001")
                                .entityType(SeoEntityType.VARIANT)
                                .pageType(SeoPageType.PDP)
                                .build();

                SeoData seoData = SeoData.builder()
                                .title("TMT Bars 8mm")
                                .build();

                SeoMeta variantMeta = new SeoMeta(
                                "TMT Bars 8mm",
                                "8mm TMT Bars",
                                "https://jswone.com/product-detail/tmt-bars-8mm/12345678-10000001",
                                "TMT Bars 8mm",
                                "product",
                                "https://jswone.com/product-detail/tmt-bars-8mm/12345678-10000001",
                                "img.jpg",
                                "8mm TMT Bars");

                when(patternFactory.resolve(variantContext)).thenReturn(mockHandler);
                when(mockHandler.generateMeta(variantContext, seoData)).thenReturn(variantMeta);

                // When
                SeoMeta result = seoService.resolveSeoMeta(variantContext, seoData);

                // Then
                assertNotNull(result);
                assertEquals("TMT Bars 8mm", result.getTitle());
                assertTrue(result.getCanonical().contains("12345678-10000001"));
        }

        @Test
        void resolveSeoMeta_shouldHandleCategoryContext() {
                // Given
                SeoContext categoryContext = SeoContext.builder()
                                .slug("steel-products")
                                .entityType(SeoEntityType.CATEGORY)
                                .pageType(SeoPageType.PLP)
                                .build();

                SeoData seoData = SeoData.builder()
                                .title("Steel Products")
                                .build();

                SeoMeta categoryMeta = new SeoMeta(
                                "Steel Products - JSW One",
                                "Browse steel products",
                                "https://jswone.com/product-listing/steel-products",
                                "Steel Products",
                                "website",
                                "https://jswone.com/product-listing/steel-products",
                                "img.jpg",
                                "Browse steel products");

                when(patternFactory.resolve(categoryContext)).thenReturn(mockHandler);
                when(mockHandler.generateMeta(categoryContext, seoData)).thenReturn(categoryMeta);

                // When
                SeoMeta result = seoService.resolveSeoMeta(categoryContext, seoData);

                // Then
                assertNotNull(result);
                assertEquals("Steel Products - JSW One", result.getTitle());
                assertEquals("website", result.getOgType());
        }

        @Test
        void resolveSeoMeta_shouldDelegateToHandler_withCorrectParameters() {
                // Given
                SeoContext context = SeoContext.builder()
                                .slug("test-product")
                                .entityType(SeoEntityType.PRODUCT)
                                .pageType(SeoPageType.PDP)
                                .build();

                SeoData data = SeoData.builder()
                                .title("Test Product")
                                .build();

                SeoMeta mockMeta = new SeoMeta(
                                "Test", "Desc", "url", "og", "product", "og-url", "img", "og-desc");

                when(patternFactory.resolve(context)).thenReturn(mockHandler);
                when(mockHandler.generateMeta(context, data)).thenReturn(mockMeta);

                // When
                seoService.resolveSeoMeta(context, data);

                // Then
                verify(patternFactory, times(1)).resolve(context);
                verify(mockHandler, times(1)).generateMeta(context, data);
                verifyNoMoreInteractions(patternFactory, mockHandler);
        }

        @Test
        void resolveSeoMeta_shouldHandleEmptySeoData() {
                // Given
                SeoContext context = SeoContext.builder()
                                .slug("product")
                                .entityType(SeoEntityType.PRODUCT)
                                .pageType(SeoPageType.PDP)
                                .build();

                SeoData emptyData = SeoData.builder().build();

                SeoMeta defaultMeta = new SeoMeta(
                                "Default Title",
                                "Default Description",
                                "url",
                                "og",
                                "product",
                                "og-url",
                                "",
                                "og-desc");

                when(patternFactory.resolve(context)).thenReturn(mockHandler);
                when(mockHandler.generateMeta(context, emptyData)).thenReturn(defaultMeta);

                // When
                SeoMeta result = seoService.resolveSeoMeta(context, emptyData);

                // Then
                assertNotNull(result);
                assertEquals("Default Title", result.getTitle());
        }

        @Test
        void resolveSeoMeta_shouldHandleComplexLocationWithVariant() {
                // Given
                SeoContext complexContext = SeoContext.builder()
                                .slug("tmt-bars-10mm")
                                .location("bangalore")
                                .variantMmid("12345678-20000002")
                                .entityType(SeoEntityType.VARIANT)
                                .pageType(SeoPageType.PDP)
                                .build();

                SeoData seoData = SeoData.builder()
                                .title("TMT Bars 10mm")
                                .image("https://example.com/tmt-10mm.jpg")
                                .build();

                SeoMeta complexMeta = new SeoMeta(
                                "TMT Bars 10mm in Bangalore",
                                "TMT Bars 10mm in Bangalore",
                                "https://jswone.com/product-detail/bangalore/tmt-bars-10mm/12345678-20000002",
                                "TMT Bars 10mm in Bangalore",
                                "product",
                                "https://jswone.com/product-detail/bangalore/tmt-bars-10mm/12345678-20000002",
                                "https://example.com/tmt-10mm.jpg",
                                "TMT Bars 10mm in Bangalore");

                when(patternFactory.resolve(complexContext)).thenReturn(mockHandler);
                when(mockHandler.generateMeta(complexContext, seoData)).thenReturn(complexMeta);

                // When
                SeoMeta result = seoService.resolveSeoMeta(complexContext, seoData);

                // Then
                assertNotNull(result);
                assertTrue(result.getTitle().contains("Bangalore"));
                assertTrue(result.getCanonical().contains("bangalore"));
                assertTrue(result.getCanonical().contains("12345678-20000002"));
                assertEquals("https://example.com/tmt-10mm.jpg", result.getOgImage());
        }

        @Test
        void resolveSeoMeta_shouldVerifyPatternFactoryInvocation() {
                // Given
                SeoContext context = SeoContext.builder()
                                .slug("test")
                                .entityType(SeoEntityType.PRODUCT)
                                .pageType(SeoPageType.PDP)
                                .build();

                SeoData data = SeoData.builder().title("Test").build();
                SeoMeta mockMeta = new SeoMeta(
                                "T", "D", "u", "o", "p", "ou", "i", "od");

                when(patternFactory.resolve(any(SeoContext.class))).thenReturn(mockHandler);
                when(mockHandler.generateMeta(any(), any())).thenReturn(mockMeta);

                // When
                seoService.resolveSeoMeta(context, data);

                // Then
                verify(patternFactory).resolve(argThat(ctx -> ctx.getSlug().equals("test") &&
                                ctx.getEntityType() == SeoEntityType.PRODUCT));
        }

        @Test
        void generateSitemap_shouldChunkAndUpload() {
                String seoBucketName = "test-bucket";
                String expectedSitemapBaseUrl = "https://qa-ssr.msme.jswone.in/api/sitemap/";
                String expectedPrefixUrl = "https://qa-ssr.msme.jswone.in";

                // Given
                com.jswone.commerce.core.model.CatalogueAttributes attributes = com.jswone.commerce.core.model.CatalogueAttributes
                                .builder()
                                .slug("cat-slug")
                                .build();

                com.jswone.commerce.core.model.CatalogueCategoryTree child = com.jswone.commerce.core.model.CatalogueCategoryTree
                                .builder()
                                .id("cat2")
                                .attributes(attributes)
                                .build();

                com.jswone.commerce.core.model.CatalogueCategoryTree tree = com.jswone.commerce.core.model.CatalogueCategoryTree
                                .builder()
                                .id("cat1")
                                .key("all_products")
                                .attributes(attributes)
                                .sub_menu(List.of(child))
                                .build();

                when(centralCatalogueClient.getCategoryTree()).thenReturn(List.of(tree));

                UrlMeta baseMeta = UrlMeta.builder().url("http://example.com/cat1").build();

                when(patternFactory.resolve(any(SeoContext.class))).thenReturn(mockHandler);
                when(mockHandler.generateUrl(any(SeoContext.class))).thenReturn(baseMeta);

                // Fix circular stubbing by returning real values
                when(commerceValueConfig.getSeoBucketName()).thenReturn(seoBucketName);
                when(commerceValueConfig.getSitemapBaseUrl()).thenReturn(expectedSitemapBaseUrl);
                when(commerceValueConfig.getJoplMsmeWebUrl()).thenReturn(expectedPrefixUrl);
                when(commerceValueConfig.getSitemapDefaultChunkSize()).thenReturn(40000);

                // Mock GCS service
                doNothing().when(gcsService).uploadFile(anyString(), anyString(), any(byte[].class), anyString(),
                                anyString());
                doNothing().when(gcsService).uploadFile(anyString(), anyString(), any(java.io.InputStream.class),
                                anyString());

                // When
                seoService.generateSitemap();

                // Then
                // Verify sitemap index upload
                verify(gcsService, times(1)).uploadFile(eq(seoBucketName), eq(SeoConstants.SITEMAP_INDEX_FILENAME),
                                any(java.io.InputStream.class),
                                eq("application/xml"));

                // Verify category sitemap upload
                // Since the list size (2) is less than chunk size (40000), it uses the base
                // name without suffix
                verify(gcsService).uploadFile(eq(seoBucketName), eq("categories-plp.xml.gz"), any(byte[].class),
                                eq("application/xml"), eq("gzip"));
        }
}
