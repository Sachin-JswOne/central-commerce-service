package com.jswone.commerce.core.rest;

import com.jswone.commerce.core.model.request.ProductBulkRequest;
import com.jswone.commerce.core.model.request.ProductTypeBulkRequest;
import com.jswone.commerce.core.model.request.Search.SearchRequest;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductBulkResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductSearchResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductTypeBulkResponse;

public interface CentralCatalogueClient {
    ProductSearchResponse genericSearch(SearchRequest searchRequest);

    ProductSearchResponse genericSearchFacetsOnly(SearchRequest searchRequest);

    ProductBulkResponse bulkMMIDResponse(ProductBulkRequest productBulkRequest);

    ProductTypeBulkResponse bulkTypeIdResponse(ProductTypeBulkRequest productTypeBulkRequest);
}
