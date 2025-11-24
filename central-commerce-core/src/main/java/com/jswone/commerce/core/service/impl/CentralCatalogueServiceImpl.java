package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.converters.CatalogueConverter;
import com.jswone.commerce.core.model.request.Search.SearchRequest;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductSearchResponse;
import com.jswone.commerce.core.model.response.search.SearchResponse;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.service.CentralCatalogueService;
import com.jswone.commerce.core.validators.CatalogueValidator;
import org.springframework.stereotype.Service;

@Service
public class CentralCatalogueServiceImpl implements CentralCatalogueService {

    private final CentralCatalogueClient centralCatalogueClient;
    private final CatalogueConverter catalogueConverter;
    private final CatalogueValidator catalogueValidator;

    public CentralCatalogueServiceImpl(CentralCatalogueClient centralCatalogueClient, CatalogueConverter catalogueConverter, CatalogueValidator catalogueValidator) {
        this.centralCatalogueClient = centralCatalogueClient;
        this.catalogueConverter = catalogueConverter;
        this.catalogueValidator = catalogueValidator;
    }

    @Override
    public SearchResponse searchCatalogue(SearchRequest searchRequest) {
        catalogueValidator.validateSearchRequest(searchRequest);
        ProductSearchResponse productSearchResponse = centralCatalogueClient.genericSearch(searchRequest);
        return catalogueConverter.convertGenericSearchToSearchResponse(productSearchResponse,searchRequest);
    }
}
