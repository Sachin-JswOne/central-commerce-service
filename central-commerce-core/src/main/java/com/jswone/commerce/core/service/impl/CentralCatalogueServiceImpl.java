package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.converters.CatalogueConverter;
import com.jswone.commerce.core.model.ImageMetadata;
import com.jswone.commerce.core.model.request.Search.SearchRequest;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductSearchResponse;
import com.jswone.commerce.core.model.response.search.SearchResponse;
import com.jswone.commerce.core.publisher.recentSearch.RecentSearchItemPublisher;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.service.CentralCatalogueService;
import com.jswone.commerce.core.util.CatalogueUtil;
import com.jswone.commerce.core.validators.CatalogueValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.jswone.commerce.core.constants.GenericConstants.BULK_IMAGE_CHUNK_SIZE;

@Service
@Slf4j
public class CentralCatalogueServiceImpl implements CentralCatalogueService {

    private final CentralCatalogueClient centralCatalogueClient;
    private final CatalogueConverter catalogueConverter;
    private final CatalogueValidator catalogueValidator;
    private final RecentSearchItemPublisher recentSearchItemPublisher;

    public CentralCatalogueServiceImpl(CentralCatalogueClient centralCatalogueClient, CatalogueConverter catalogueConverter, CatalogueValidator catalogueValidator, RecentSearchItemPublisher recentSearchItemPublisher) {
        this.centralCatalogueClient = centralCatalogueClient;
        this.catalogueConverter = catalogueConverter;
        this.catalogueValidator = catalogueValidator;
        this.recentSearchItemPublisher = recentSearchItemPublisher;
    }

    @Override
    public SearchResponse searchCatalogue(SearchRequest searchRequest) {
        catalogueValidator.validateSearchRequest(searchRequest);
        ProductSearchResponse productSearchResponse = centralCatalogueClient.genericSearch(searchRequest);
        recentSearchItemPublisher.publish(productSearchResponse, searchRequest);
        ProductSearchResponse facetsResponse = centralCatalogueClient.genericSearchFacetsOnly(searchRequest);
        return catalogueConverter.convertGenericSearchToSearchResponse(productSearchResponse, facetsResponse, searchRequest);
    }

    @Override
    public Map<String, ImageMetadata> fetchImagesForMmIds(Set<String> productMmIds) {

        Map<String, ImageMetadata> finalImageMap = new HashMap<>();

        List<Set<String>> batches = CatalogueUtil.chunkSet(productMmIds, BULK_IMAGE_CHUNK_SIZE);

        log.info("Total batches to process: {}", batches.size());

        for (int i = 0; i < batches.size(); i++) {
            Set<String> batch = batches.get(i);
            log.info("Processing batch {} of size {}", i + 1, batch.size());

            Map<String, ImageMetadata> batchResult = centralCatalogueClient.fetchImagesForMmIds(batch);
            finalImageMap.putAll(batchResult);
        }
        log.info("Successfully processed {} MMIDs in {} batches", finalImageMap.size(), batches.size());
        return finalImageMap;
    }
}
