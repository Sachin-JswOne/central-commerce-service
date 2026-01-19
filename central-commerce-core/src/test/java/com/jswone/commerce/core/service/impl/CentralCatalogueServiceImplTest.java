package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.converters.CatalogueConverter;
import com.jswone.commerce.core.model.request.Search.SearchRequest;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductSearchResponse;
import com.jswone.commerce.core.model.response.search.SearchResponse;
import com.jswone.commerce.core.publisher.recentSearch.RecentSearchItemPublisher;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.validators.CatalogueValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private RecentSearchItemPublisher recentSearchItemPublisher;

    @InjectMocks
    private CentralCatalogueServiceImpl centralCatalogueService;

    // -------------------- POSITIVE CASE --------------------

    @Test
    void searchCatalogue_successfulFlow() {
        SearchRequest searchRequest = mock(SearchRequest.class);
        ProductSearchResponse productSearchResponse = mock(ProductSearchResponse.class);
        ProductSearchResponse facetsResponse = mock(ProductSearchResponse.class);
        SearchResponse searchResponse = mock(SearchResponse.class);

        when(centralCatalogueClient.genericSearch(searchRequest))
                .thenReturn(productSearchResponse);

        when(centralCatalogueClient.genericSearchFacetsOnly(searchRequest))
                .thenReturn(facetsResponse);

        when(catalogueConverter.convertGenericSearchToSearchResponse(
                productSearchResponse, facetsResponse, searchRequest))
                .thenReturn(searchResponse);

        SearchResponse result = centralCatalogueService.searchCatalogue(searchRequest);

        assertNotNull(result);
        assertEquals(searchResponse, result);

        verify(catalogueValidator).validateSearchRequest(searchRequest);
        verify(centralCatalogueClient).genericSearch(searchRequest);
        verify(recentSearchItemPublisher).publish(productSearchResponse, searchRequest);
        verify(centralCatalogueClient).genericSearchFacetsOnly(searchRequest);
        verify(catalogueConverter).convertGenericSearchToSearchResponse(
                productSearchResponse, facetsResponse, searchRequest);
    }

    // -------------------- NEGATIVE CASES --------------------

    @Test
    void searchCatalogue_validationFailure_shouldThrowException() {
        SearchRequest searchRequest = mock(SearchRequest.class);

        doThrow(new IllegalArgumentException("Invalid search request"))
                .when(catalogueValidator)
                .validateSearchRequest(searchRequest);

        assertThrows(IllegalArgumentException.class,
                () -> centralCatalogueService.searchCatalogue(searchRequest));

        verify(catalogueValidator).validateSearchRequest(searchRequest);
        verifyNoInteractions(centralCatalogueClient, catalogueConverter, recentSearchItemPublisher);
    }

    @Test
    void searchCatalogue_genericSearchFailure_shouldPropagateException() {
        SearchRequest searchRequest = mock(SearchRequest.class);

        when(centralCatalogueClient.genericSearch(searchRequest))
                .thenThrow(new RuntimeException("Catalogue service error"));

        assertThrows(RuntimeException.class,
                () -> centralCatalogueService.searchCatalogue(searchRequest));

        verify(catalogueValidator).validateSearchRequest(searchRequest);
        verify(centralCatalogueClient).genericSearch(searchRequest);
        verifyNoInteractions(catalogueConverter);
    }

    @Test
    void searchCatalogue_facetsSearchFailure_shouldPropagateException() {
        SearchRequest searchRequest = mock(SearchRequest.class);
        ProductSearchResponse productSearchResponse = mock(ProductSearchResponse.class);

        when(centralCatalogueClient.genericSearch(searchRequest))
                .thenReturn(productSearchResponse);

        when(centralCatalogueClient.genericSearchFacetsOnly(searchRequest))
                .thenThrow(new RuntimeException("Facet API failed"));

        assertThrows(RuntimeException.class,
                () -> centralCatalogueService.searchCatalogue(searchRequest));

        verify(centralCatalogueClient).genericSearch(searchRequest);
        verify(recentSearchItemPublisher).publish(productSearchResponse, searchRequest);
        verify(centralCatalogueClient).genericSearchFacetsOnly(searchRequest);
        verifyNoInteractions(catalogueConverter);
    }
}
