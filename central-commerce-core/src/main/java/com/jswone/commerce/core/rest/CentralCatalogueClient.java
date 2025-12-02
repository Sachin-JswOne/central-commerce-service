package com.jswone.commerce.core.rest;

import com.jswone.commerce.core.model.ImageMetadata;
import com.jswone.commerce.core.model.request.ProductBulkRequest;
import com.jswone.commerce.core.model.request.ProductTypeBulkRequest;
import com.jswone.commerce.core.model.request.Search.SearchRequest;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductBulkResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductSearchResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductTypeBulkResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CentralCatalogueClient {
    ProductSearchResponse genericSearch(SearchRequest searchRequest);
    ProductBulkResponse bulkMMIDResponse(ProductBulkRequest productBulkRequest);
    ProductTypeBulkResponse bulkTypeIdResponse(ProductTypeBulkRequest productTypeBulkRequest);
    Map<String, ImageMetadata> fetchImagesForMmIds(Set<String> productMmIds);
}
