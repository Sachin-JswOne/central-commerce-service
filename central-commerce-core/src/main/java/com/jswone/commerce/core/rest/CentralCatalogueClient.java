package com.jswone.commerce.core.rest;

import com.jswone.commerce.core.model.ImageMetadata;
import com.jswone.commerce.core.model.request.ProductBulkRequest;
import com.jswone.commerce.core.model.request.Search.SearchRequest;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductBulkResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductSearchResponse;

import java.util.List;
import java.util.Map;

public interface CentralCatalogueClient {
    ProductSearchResponse genericSearch(SearchRequest searchRequest);
    ProductBulkResponse bulkMMIDResponse(ProductBulkRequest productBulkRequest);
    Map<String, ImageMetadata> fetchImagesForMmIds(List<String> productMmIds);
}
