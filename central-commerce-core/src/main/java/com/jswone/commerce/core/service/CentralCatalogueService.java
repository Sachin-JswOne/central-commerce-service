package com.jswone.commerce.core.service;

import com.jswone.commerce.core.model.ImageMetadata;
import com.jswone.commerce.core.model.request.Search.SearchRequest;
import com.jswone.commerce.core.model.response.search.SearchResponse;

import java.util.List;
import java.util.Map;

public interface CentralCatalogueService {
    SearchResponse searchCatalogue(SearchRequest searchRequest);
    Map<String, ImageMetadata> fetchImagesForMmIds(List<String> productMmIds);
}
