package com.jswone.commerce.core.service;

import com.jswone.commerce.core.model.request.Search.SearchRequest;
import com.jswone.commerce.core.model.response.search.SearchResponse;

public interface CentralCatalogueService {
    SearchResponse searchCatalogue(SearchRequest searchRequest);
}
