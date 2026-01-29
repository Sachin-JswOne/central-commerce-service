package com.jswone.commerce.core.service;

import com.jswone.commerce.core.model.request.ProductListingRequest;
import com.jswone.commerce.core.model.request.Search.SearchRequest;
import com.jswone.commerce.core.model.response.ProductListingResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductBulkResponse;
import com.jswone.commerce.core.model.response.search.SearchResponse;

import java.util.Set;

public interface CentralCatalogueService {
    SearchResponse searchCatalogue(SearchRequest searchRequest);
    ProductListingResponse productListing(ProductListingRequest productListingRequest);
    ProductBulkResponse fetchProductsByProductMMIDs(Set<String> productMmIds);
}
