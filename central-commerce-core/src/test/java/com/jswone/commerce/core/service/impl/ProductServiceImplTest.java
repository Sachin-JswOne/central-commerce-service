package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.entity.catalogue.Attribute;
import com.jswone.commerce.core.entity.catalogue.ProductCatalogueStore;
import com.jswone.commerce.core.entity.catalogue.Variant;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.exceptions.ProductSelectorException;
import com.jswone.commerce.core.mapper.ProductSlugMapper;
import com.jswone.commerce.core.model.centralCatalogue.*;
import com.jswone.commerce.core.model.request.ProductAttributeDTO;
import com.jswone.commerce.core.model.request.ProductSkuRequest;
import com.jswone.commerce.core.model.response.SkuInfo;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductBulkResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductTypeBulkDTO;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductTypeBulkResponse;
import com.jswone.commerce.core.model.seo.SeoContext;
import com.jswone.commerce.core.model.seo.SeoData;
import com.jswone.commerce.core.model.seo.SeoMeta;
import com.jswone.commerce.core.repository.ProductCatalogueStoreRepository;
import com.jswone.commerce.core.resolver.SeoContextResolver;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.service.SeoService;
import com.jswone.commerce.core.util.ProductAttributeUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

        @Mock
        private ProductCatalogueStoreRepository productCatalogueStoreRepository;

        @Mock
        private ProductSlugMapper productSlugMapper;

        @Mock
        private CentralCatalogueClient centralCatalogueClient;

        @Mock
        private ProductAttributeUtil productAttributeUtil;

        @Mock
        private SeoService seoService;

        @Mock
        private SeoContextResolver seoContextResolver;

        @InjectMocks
        private ProductServiceImpl productService;

        @BeforeEach
        void setUp() {
                // Set up mock SEO metadata (lenient for tests that don't use it)
                SeoMeta mockSeoMeta = new SeoMeta("Test Title", "https://example.com/",
                                "https://example.com/canonical", "OG Title", "product",
                                "https://example.com/og", "https://example.com/image.jpg", "OG Description");
                lenient().when(seoService.resolveSeoMeta(any(SeoContext.class), any(SeoData.class)))
                                .thenReturn(mockSeoMeta);

                // Mock both resolve signatures - old (backward compatible) and new (with entity
                // type)
                lenient().when(seoContextResolver.resolve(any(String.class)))
                                .thenReturn(SeoContext.builder().build());
                lenient().when(seoContextResolver.resolve(any(String.class), any()))
                                .thenReturn(SeoContext.builder().build());
        }

        // Helper Methods

        private ProductSkuRequest buildRequest() {
                return ProductSkuRequest.builder()
                                .productMaterialMasterId("PMM-1")
                                .productAttributes(List.of(
                                                ProductAttributeDTO.builder()
                                                                .key("COLOR")
                                                                .value("RED")
                                                                .build()))
                                .build();
        }

        private ProductCatalogueStore buildStoreWithSingleMatchingVariant() {
                Variant variant = new Variant();
                variant.setVariantKey("V1");
                variant.setSku("SKU1");
                variant.setMmId("MM1");
                variant.setAttributes(List.of(
                                Attribute.builder().name("COLOR").value("RED").build()));

                ProductCatalogueStore store = new ProductCatalogueStore();
                store.setProductMaterialMasterId("PMM-1");
                store.setProductKey("PK1");
                store.setProductTypeKey("PT1");
                store.setVariants(List.of(variant));
                store.setMasterVariant(variant);

                return store;
        }

        // getMatchedVariantResponse

        @Test
        void getMatchedVariantResponse_shouldReturnSku_whenSingleVariantMatches() {
                ProductSkuRequest request = buildRequest();
                ProductCatalogueStore store = buildStoreWithSingleMatchingVariant();

                when(productAttributeUtil.convertAttributes(any()))
                                .thenReturn(request.getProductAttributes());

                when(productAttributeUtil.transformToAttribute(any()))
                                .thenAnswer(invocation -> {
                                        ProductAttributeDTO dto = invocation.getArgument(0);
                                        return Attribute.builder()
                                                        .name(dto.getKey())
                                                        .value(dto.getValue())
                                                        .build();
                                });

                when(productCatalogueStoreRepository
                                .findProductCatalogueStoresByProductMaterialMasterId("PMM-1"))
                                .thenReturn(store);

                SkuInfo skuInfo = productService.getMatchedVariantResponse(request);

                assertNotNull(skuInfo);
                assertEquals("SKU1", skuInfo.getSku());
                assertEquals("V1", skuInfo.getVariantKey());
        }

        @Test
        void getMatchedVariantResponse_shouldThrowException_whenStoreIsNull() {
                ProductSkuRequest request = buildRequest();

                when(productAttributeUtil.convertAttributes(any()))
                                .thenReturn(request.getProductAttributes());
                when(productCatalogueStoreRepository
                                .findProductCatalogueStoresByProductMaterialMasterId(any()))
                                .thenReturn(null);

                assertThrows(
                                CentralCommerceServiceException.class,
                                () -> productService.getMatchedVariantResponse(request));
        }

        // getMatchedVariant internal logic

        @Test
        void shouldReturnMasterSku_whenNoVariantMatches() {
                ProductSkuRequest request = buildRequest();
                ProductCatalogueStore store = buildStoreWithSingleMatchingVariant();

                // Variant does NOT match request
                store.getVariants().getFirst()
                                .setAttributes(List.of(
                                                Attribute.builder().name("COLOR").value("BLUE").build()));

                when(productAttributeUtil.convertAttributes(any()))
                                .thenReturn(request.getProductAttributes());

                when(productAttributeUtil.transformToAttribute(any()))
                                .thenAnswer(invocation -> {
                                        ProductAttributeDTO dto = invocation.getArgument(0);
                                        return Attribute.builder()
                                                        .name(dto.getKey())
                                                        .value(dto.getValue())
                                                        .build();
                                });

                when(productCatalogueStoreRepository
                                .findProductCatalogueStoresByProductMaterialMasterId(any()))
                                .thenReturn(store);

                SkuInfo skuInfo = productService.getMatchedVariantResponse(request);

                assertNotNull(skuInfo);
                assertEquals("SKU1", skuInfo.getSku()); // master SKU
        }

        @Test
        void shouldThrowException_whenMultipleVariantsMatch() {
                ProductSkuRequest request = buildRequest();
                ProductCatalogueStore store = buildStoreWithSingleMatchingVariant();

                Variant secondVariant = new Variant();
                secondVariant.setVariantKey("V2");
                secondVariant.setSku("SKU2");
                secondVariant.setAttributes(List.of(
                                Attribute.builder().name("COLOR").value("RED").build()));

                store.setVariants(List.of(store.getVariants().getFirst(), secondVariant));

                when(productAttributeUtil.convertAttributes(any()))
                                .thenReturn(request.getProductAttributes());
                when(productCatalogueStoreRepository
                                .findProductCatalogueStoresByProductMaterialMasterId(any()))
                                .thenReturn(store);

                assertThrows(
                                ProductSelectorException.class,
                                () -> productService.getMatchedVariantResponse(request));
        }

        // getVariant

        @Test
        void getVariant_shouldMatchRangeBasedAttribute() {
                Attribute inputAttribute = Attribute.builder()
                                .name("LENGTH_MM")
                                .value(50)
                                .build();

                Variant variant = new Variant();
                variant.setAttributes(List.of(
                                Attribute.builder().name("LENGTH_MIN_MM").value(40).build(),
                                Attribute.builder().name("LENGTH_MAX_MM").value(60).build()));

                Pair<Boolean, List<Attribute>> result = ProductServiceImpl.checkForRangeBasedMatch(
                                List.of(inputAttribute), variant);

                assertTrue(result.getLeft());
                assertEquals(1, result.getRight().size());
        }

        // isMatchProductVariant

        @Test
        void isMatchProductVariant_shouldReturnTrue_whenExactMatch() {
                Variant variant = new Variant();
                variant.setAttributes(List.of(
                                Attribute.builder().name("SIZE").value("L").build()));

                Pair<Boolean, List<Attribute>> result = ProductServiceImpl.isMatchProductVariant(
                                variant,
                                List.of(Attribute.builder().name("SIZE").value("L").build()));

                assertTrue(result.getLeft());
        }

        @Test
        void isMatchProductVariant_shouldReturnFalse_whenVariantIsNull() {
                Pair<Boolean, List<Attribute>> result = ProductServiceImpl.isMatchProductVariant(null, List.of());

                assertFalse(result.getLeft());
        }

        // checkEquality

        @Test
        void checkEquality_shouldReturnTrue_forNumericMatch() throws Exception {
                Attribute a1 = Attribute.builder().name("QTY").value(10).build();
                Attribute a2 = Attribute.builder().name("QTY").value(10).build();

                assertTrue(invokeCheckEquality(a1, a2));
        }

        @Test
        void checkEquality_shouldReturnFalse_forDifferentValues() throws Exception {
                Attribute a1 = Attribute.builder().name("QTY").value(10).build();
                Attribute a2 = Attribute.builder().name("QTY").value(20).build();

                assertFalse(invokeCheckEquality(a1, a2));
        }

        private boolean invokeCheckEquality(Attribute a, Attribute b) throws Exception {
                Method method = ProductServiceImpl.class
                                .getDeclaredMethod("checkEquality", Attribute.class, Attribute.class);
                method.setAccessible(true);
                return (boolean) method.invoke(null, a, b);
        }

        @Test
        void shouldThrowException_whenProductAttributesAreNull() {
                ProductSkuRequest request = ProductSkuRequest.builder()
                                .productMaterialMasterId("PMM-1")
                                .productAttributes(null)
                                .build();

                assertThrows(
                                CentralCommerceServiceException.class,
                                () -> productService.getMatchedVariantResponse(request));
        }

        @Test
        void shouldReturnMasterSku_whenVariantsAreNull() {
                ProductSkuRequest request = buildRequest();
                ProductCatalogueStore store = buildStoreWithSingleMatchingVariant();
                store.setVariants(null); // 👈 EDGE

                when(productAttributeUtil.convertAttributes(any()))
                                .thenReturn(request.getProductAttributes());
                when(productAttributeUtil.transformToAttribute(any()))
                                .thenAnswer(invocation -> {
                                        ProductAttributeDTO dto = invocation.getArgument(0);
                                        return Attribute.builder()
                                                        .name(dto.getKey())
                                                        .value(dto.getValue())
                                                        .build();
                                });

                when(productCatalogueStoreRepository
                                .findProductCatalogueStoresByProductMaterialMasterId(any()))
                                .thenReturn(store);

                SkuInfo skuInfo = productService.getMatchedVariantResponse(request);

                assertNotNull(skuInfo);
                assertEquals("SKU1", skuInfo.getSku());
        }

        @Test
        void shouldThrowException__whenProductAttributesAreNull() {
                ProductSkuRequest request = ProductSkuRequest.builder()
                                .productMaterialMasterId("PMM-1")
                                .productAttributes(null)
                                .build();

                assertThrows(
                                CentralCommerceServiceException.class,
                                () -> productService.getMatchedVariantResponse(request));
        }

        @Test
        void shouldHandleNullMasterVariantGracefully() {
                ProductSkuRequest request = buildRequest();
                ProductCatalogueStore store = buildStoreWithSingleMatchingVariant();
                store.setMasterVariant(null); // 👈 EDGE CASE

                when(productAttributeUtil.convertAttributes(any()))
                                .thenReturn(request.getProductAttributes());
                when(productAttributeUtil.transformToAttribute(any()))
                                .thenAnswer(invocation -> {
                                        ProductAttributeDTO dto = invocation.getArgument(0);
                                        return Attribute.builder()
                                                        .name(dto.getKey())
                                                        .value(dto.getValue())
                                                        .build();
                                });
                when(productCatalogueStoreRepository
                                .findProductCatalogueStoresByProductMaterialMasterId(any()))
                                .thenReturn(store);

                SkuInfo skuInfo = productService.getMatchedVariantResponse(request);

                assertNotNull(skuInfo);
                assertEquals("SKU1", skuInfo.getSku());
        }

        @Test
        void shouldGenerateDefaultVariantMMId_whenMmIdIsNull() {
                ProductSkuRequest request = buildRequest();

                Variant variant = new Variant();
                variant.setVariantKey("V1");
                variant.setSku("SKU1");
                variant.setMmId(null); // 👈 EDGE
                variant.setAttributes(List.of(
                                Attribute.builder().name("COLOR").value("RED").build()));

                ProductCatalogueStore store = new ProductCatalogueStore();
                store.setProductMaterialMasterId("PMM-1");
                store.setVariants(List.of(variant));
                store.setMasterVariant(variant);

                when(productAttributeUtil.convertAttributes(any()))
                                .thenReturn(request.getProductAttributes());
                when(productAttributeUtil.transformToAttribute(any()))
                                .thenAnswer(invocation -> {
                                        ProductAttributeDTO dto = invocation.getArgument(0);
                                        return Attribute.builder()
                                                        .name(dto.getKey())
                                                        .value(dto.getValue())
                                                        .build();
                                });
                when(productCatalogueStoreRepository
                                .findProductCatalogueStoresByProductMaterialMasterId(any()))
                                .thenReturn(store);

                SkuInfo sku = productService.getMatchedVariantResponse(request);

                assertTrue(sku.getVariantMMID().contains("10000000"));
        }

        @Test
        void isMatchProductVariant_shouldReturnTrue_whenAttributesEmpty() {
                Variant variant = new Variant();
                variant.setAttributes(List.of(
                                Attribute.builder().name("COLOR").value("RED").build()));

                Pair<Boolean, List<Attribute>> result = ProductServiceImpl.isMatchProductVariant(variant, List.of());

                assertTrue(result.getLeft()); // documents behavior
        }

        @Test
        void rangeMatch_shouldMatchBoundaryValues() {
                Attribute input = Attribute.builder()
                                .name("WEIGHT_KG")
                                .value(10)
                                .build();

                Variant variant = new Variant();
                variant.setAttributes(List.of(
                                Attribute.builder().name("WEIGHT_MIN_KG").value(10).build(),
                                Attribute.builder().name("WEIGHT_MAX_KG").value(20).build()));

                Pair<Boolean, List<Attribute>> result = ProductServiceImpl.checkForRangeBasedMatch(
                                List.of(input), variant);

                assertTrue(result.getLeft());
        }

        @Test
        void shouldWrapRuntimeExceptionIntoProductSelectorException() {
                ProductSkuRequest request = buildRequest();

                when(productCatalogueStoreRepository
                                .findProductCatalogueStoresByProductMaterialMasterId(any()))
                                .thenThrow(new RuntimeException("DB down"));

                assertThrows(
                                ProductSelectorException.class,
                                () -> productService.getMatchedVariantResponse(request));
        }

        @Test
        void getProductFromSlug_shouldReturnProductSlug_whenValidResponse() {
                String slug = "tmt-bars";
                String productTypeId = "PT1";

                // -------- Product --------
                Product product = mock(Product.class);
                when(product.getProductTypeId()).thenReturn(productTypeId);

                ProductBulkResponse bulkResponse = mock(ProductBulkResponse.class);
                when(bulkResponse.getProducts()).thenReturn(List.of(product));

                // -------- Quantity Cards --------
                QuantityCard quantityCard = mock(QuantityCard.class);
                List<QuantityCard> quantityCards = List.of(quantityCard);

                // -------- ProductTypeData (IMPORTANT) --------
                ProductTypeData productTypeData = mock(ProductTypeData.class);
                when(productTypeData.getQuantityCards()).thenReturn(quantityCards);
                when(productTypeData.getVariantSelectors()).thenReturn(Map.of());
                when(productTypeData.getStandardAttributes()).thenReturn(List.of());
                when(productTypeData.getAttributes()).thenReturn(List.of());

                Map<String, ProductTypeData> productTypeMap = Map.of(productTypeId, productTypeData);

                // -------- ProductTypeBulkDTO --------
                ProductTypeBulkDTO bulkDTO = mock(ProductTypeBulkDTO.class);
                when(bulkDTO.getProductTypeDetail()).thenReturn(productTypeMap);
                when(bulkDTO.getProductOverview()).thenReturn(List.of());

                ProductTypeBulkResponse typeBulkResponse = new ProductTypeBulkResponse(200, "SUCCESS", bulkDTO);

                ProductSlug mappedSlug = new ProductSlug();

                lenient().when(centralCatalogueClient.getProductFromSlug(any(), any()))
                                .thenReturn(bulkResponse);
                when(centralCatalogueClient.bulkTypeIdResponse(any()))
                                .thenReturn(typeBulkResponse);
                when(productSlugMapper.toProductSlug(product, quantityCards))
                                .thenReturn(mappedSlug);

                // -------- Call --------
                ProductSlug result = productService.getProductFromSlug(slug, "msme");

                // -------- Assert --------
                assertNotNull(result);
                assertEquals(mappedSlug, result);
        }

        @Test
        void getProductFromSlug_shouldThrowException_whenProductBulkResponseIsNull() {
                lenient().when(centralCatalogueClient.getProductFromSlug(any(), any()))
                                .thenReturn(null);

                assertThrows(
                                CentralCommerceServiceException.class,
                                () -> productService.getProductFromSlug("slug", "msme"));
        }

        @Test
        void getProductFromSlug_shouldThrowException_whenProductsEmpty() {
                ProductBulkResponse response = new ProductBulkResponse();
                response.setProducts(List.of());

                when(centralCatalogueClient.getProductFromSlug(any(), any()))
                                .thenReturn(response);

                assertThrows(
                                CentralCommerceServiceException.class,
                                () -> productService.getProductFromSlug("slug", "msme"));
        }

        @Test
        void getProductFromSlug_shouldThrowException_whenProductTypeIdNull() {
                Product product = new Product();
                product.setProductTypeId(null);

                ProductBulkResponse response = new ProductBulkResponse();
                response.setProducts(List.of(product));

                when(centralCatalogueClient.getProductFromSlug(any(), any()))
                                .thenReturn(response);

                assertThrows(
                                CentralCommerceServiceException.class,
                                () -> productService.getProductFromSlug("slug", "msme"));
        }

        @Test
        void getProductFromSlug_shouldThrowException_whenProductTypeBulkResponseNull() {
                Product product = new Product();
                product.setProductTypeId("PT1");

                ProductBulkResponse response = new ProductBulkResponse();
                response.setProducts(List.of(product));

                when(centralCatalogueClient.getProductFromSlug(any(), any()))
                                .thenReturn(response);
                when(centralCatalogueClient.bulkTypeIdResponse(any()))
                                .thenReturn(null);

                assertThrows(
                                CentralCommerceServiceException.class,
                                () -> productService.getProductFromSlug("slug", "msme"));
        }

        @Test
        void getProductFromSlug_shouldThrowException_whenQuantityCardsNull() {
                // -------- Product --------
                Product product = new Product();
                product.setProductTypeId("PT1");

                ProductBulkResponse bulkResponse = new ProductBulkResponse();
                bulkResponse.setProducts(List.of(product));

                // -------- ProductTypeData (CORRECT CLASS) --------
                ProductTypeData productTypeData = new ProductTypeData();
                productTypeData.setQuantityCards(null); // 👈 this is what we are testing

                Map<String, ProductTypeData> productTypeMap = Map.of("PT1", productTypeData);

                // -------- ProductTypeBulkDTO (CORRECT CLASS) --------
                ProductTypeBulkDTO bulkDTO = new ProductTypeBulkDTO();
                bulkDTO.setProductTypeDetail(productTypeMap);
                bulkDTO.setProductOverview(List.of());

                // -------- ProductTypeBulkResponse --------
                ProductTypeBulkResponse typeBulkResponse = new ProductTypeBulkResponse();
                typeBulkResponse.setData(bulkDTO);

                // -------- Stubbing --------
                lenient().when(centralCatalogueClient.getProductFromSlug(any(), any()))
                                .thenReturn(bulkResponse);
                lenient().when(centralCatalogueClient.bulkTypeIdResponse(any()))
                                .thenReturn(typeBulkResponse);

                // -------- Assert --------
                assertThrows(
                                CentralCommerceServiceException.class,
                                () -> productService.getProductFromSlug("slug", "msme"));
        }

        @Test
        void getProductFromSlug_shouldWrapAnyException() {
                when(centralCatalogueClient.getProductFromSlug(any(), any()))
                                .thenThrow(new RuntimeException("Service down"));

                CentralCommerceServiceException ex = assertThrows(
                                CentralCommerceServiceException.class,
                                () -> productService.getProductFromSlug("slug", "msme"));

                assertTrue(ex.getMessage().contains("Service down"));
        }

}
