package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.converters.CatalogueConverter;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.CategoryTreeResponse;
import com.jswone.commerce.core.model.ImageMetadata;
import com.jswone.commerce.core.model.request.ProductBulkRequest;
import com.jswone.commerce.core.model.request.ProductListingRequest;
import com.jswone.commerce.core.model.request.Search.SearchRequest;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductBulkResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductSearchResponse;
import com.jswone.commerce.core.model.response.plp.ProductFilterConditions;
import com.jswone.commerce.core.model.response.search.SearchResponse;
import com.jswone.commerce.core.publisher.recentSearch.UserSearchLogsItemPublisher;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.service.CatalogueCategoryService;
import com.jswone.commerce.core.validators.CatalogueValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CentralCatalogueServiceImplTest {

    @Mock
    private CentralCatalogueClient centralCatalogueClient;

    @Mock
    private CatalogueConverter catalogueConverter;

    @Mock
    private CatalogueValidator catalogueValidator;

    @Mock
    private UserSearchLogsItemPublisher recentSearchItemPublisher;

    @Mock
    private CatalogueCategoryService catalogueCategoryService;

    @InjectMocks
    private CentralCatalogueServiceImpl service;

    /* -------------------- searchCatalogue -------------------- */

    @Test
    void searchCatalogue_success() {
        SearchRequest request = mock(SearchRequest.class);
        ProductSearchResponse searchResponse = mock(ProductSearchResponse.class);
        SearchResponse finalResponse = mock(SearchResponse.class);
        CategoryTreeResponse categoryTreeResponse = mock(CategoryTreeResponse.class);
        ProductFilterConditions filterConditions = mock(ProductFilterConditions.class);

        when(centralCatalogueClient.genericSearch(request)).thenReturn(searchResponse);
        SearchResponse result = service.searchCatalogue(request);

        assertNotNull(result);
        verify(catalogueValidator).validateSearchRequest(request);
    }

    @Test
    void searchCatalogue_validationFailure() {
        SearchRequest request = mock(SearchRequest.class);

        doThrow(new IllegalArgumentException("Invalid"))
                .when(catalogueValidator).validateSearchRequest(request);

        assertThrows(IllegalArgumentException.class,
                () -> service.searchCatalogue(request));

        verifyNoInteractions(centralCatalogueClient, catalogueConverter, recentSearchItemPublisher);
    }

    /* -------------------- fetchImagesForMmIds -------------------- */

    @Test
    void fetchImagesForMmIds_singleBatch() {
        Set<String> mmIds = Set.of("MM1", "MM2");
        ImageMetadata metadata = mock(ImageMetadata.class);

        when(centralCatalogueClient.fetchImagesForMmIds(anySet()))
                .thenReturn(Map.of("MM1", metadata, "MM2", metadata));

        Map<String, ImageMetadata> result = service.fetchImagesForMmIds(mmIds);

        assertEquals(2, result.size());
        verify(centralCatalogueClient).fetchImagesForMmIds(anySet());
    }

    @Test
    void fetchImagesForMmIds_multipleBatches() {
        Set<String> mmIds = new HashSet<>();
        for (int i = 0; i < 25; i++) {
            mmIds.add("MM" + i);
        }

        when(centralCatalogueClient.fetchImagesForMmIds(anySet()))
                .thenReturn(Collections.emptyMap());

        Map<String, ImageMetadata> result = service.fetchImagesForMmIds(mmIds);

        assertNotNull(result);
        verify(centralCatalogueClient, atLeastOnce()).fetchImagesForMmIds(anySet());
    }

    /* -------------------- productListing -------------------- */

//    @Test
//    void productListing_success_withCategoryId() {
//        ProductListingRequest request = mock(ProductListingRequest.class);
//        ProductListingCatalogueResponse catalogueResponse = mock(ProductListingCatalogueResponse.class);
//        ProductListingResponse finalResponse = mock(ProductListingResponse.class);
//        CategoryTreeResponse categoryTreeResponse = mock(CategoryTreeResponse.class);
//
//        when(request.getCategoryId()).thenReturn("CAT123");
//        when(request.getSlug()).thenReturn(null);
//
//        when(centralCatalogueClient.productListing(request)).thenReturn(catalogueResponse);
//        when(catalogueCategoryService.getBulkCatalogueCategoryTree(any())).thenReturn(categoryTreeResponse);
//        when(catalogueConverter.convertCataloguePLPResponseToPLPResponse(
//                catalogueResponse, request, categoryTreeResponse, null)).thenReturn(finalResponse);
//
//        ProductListingResponse response = service.productListing(request);
//
//        assertNotNull(response);
//        verify(catalogueValidator).validateProductListingRequest(request);
//    }

    @Test
    void productListing_missingCategoryAndSlug_shouldThrowException() {
        ProductListingRequest request = mock(ProductListingRequest.class);
        when(request.getCategoryId()).thenReturn(null);
        when(request.getSlug()).thenReturn(null);

        assertThrows(CentralCommerceServiceException.class,
                () -> service.productListing(request));
    }

    @Test
    void productListing_bothCategoryAndSlugPresent_shouldThrowException() {
        ProductListingRequest request = mock(ProductListingRequest.class);
        when(request.getCategoryId()).thenReturn("CAT1");
        when(request.getSlug()).thenReturn("slug");

        assertThrows(CentralCommerceServiceException.class,
                () -> service.productListing(request));
    }

    /* -------------------- fetchProductsByProductMMIDs -------------------- */

    @Test
    void fetchProductsByProductMMIDs_nullInput() {
        ProductBulkResponse response = service.fetchProductsByProductMMIDs(null);

        assertNotNull(response);
        verifyNoInteractions(centralCatalogueClient);
    }

    @Test
    void fetchProductsByProductMMIDs_emptyInput() {
        ProductBulkResponse response = service.fetchProductsByProductMMIDs(Collections.emptySet());

        assertNotNull(response);
        verifyNoInteractions(centralCatalogueClient);
    }

    @Test
    void fetchProductsByProductMMIDs_success() {
        Set<String> mmIds = Set.of("MM1", "MM2");
        ProductBulkResponse bulkResponse = new ProductBulkResponse(Collections.emptyList(), 2);

        when(centralCatalogueClient.bulkMMIDResponse(any(ProductBulkRequest.class)))
                .thenReturn(bulkResponse);

        ProductBulkResponse response = service.fetchProductsByProductMMIDs(mmIds);

        assertNotNull(response);
        verify(centralCatalogueClient).bulkMMIDResponse(any(ProductBulkRequest.class));
    }

    @Test
    void fetchProductsByProductMMIDs_exceptionHandledGracefully() {
        Set<String> mmIds = Set.of("MM1");

        when(centralCatalogueClient.bulkMMIDResponse(any(ProductBulkRequest.class)))
                .thenThrow(new RuntimeException("Downstream failure"));

        ProductBulkResponse response = service.fetchProductsByProductMMIDs(mmIds);

        assertNotNull(response);
        verify(centralCatalogueClient).bulkMMIDResponse(any(ProductBulkRequest.class));
    }
}
