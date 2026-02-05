package com.jswone.commerce.core.service;

import com.jswone.commerce.core.model.ImageMetadata;
import com.jswone.commerce.core.model.request.ProductListingRequest;
import com.jswone.commerce.core.model.request.Search.SearchRequest;
import com.jswone.commerce.core.model.response.ProductListingResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductBulkResponse;
import com.jswone.commerce.core.model.response.search.SearchResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CentralCatalogueService {
    SearchResponse searchCatalogue(SearchRequest searchRequest);

    Map<String, ImageMetadata> fetchImagesForMmIds(Set<String> productMmIds);

    ProductListingResponse productListing(ProductListingRequest productListingRequest);
    Map<String, ProductListingResponse> productListingBulk(
            List<String> slugs
    );

    ProductBulkResponse fetchProductsByProductMMIDs(Set<String> productMmIds);
}
