package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.converters.CatalogueConverter;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.request.ProductListingRequest;
import com.jswone.commerce.core.model.request.Search.SearchRequest;
import com.jswone.commerce.core.model.response.ProductListingResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductListingCatalogueResponse;
import com.jswone.commerce.core.model.request.ProductBulkRequest;
import com.jswone.commerce.core.model.request.Search.SearchRequest;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductBulkResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductSearchResponse;
import com.jswone.commerce.core.model.response.search.SearchResponse;
import com.jswone.commerce.core.publisher.recentSearch.RecentSearchItemPublisher;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.service.CentralCatalogueService;
import com.jswone.commerce.core.validators.CatalogueValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

import static com.jswone.commerce.core.constants.BuyAgainConstants.LOCALE_EN_US;

@Service
@Slf4j
public class CentralCatalogueServiceImpl implements CentralCatalogueService {

    private static final String STOREFRONT_MSME = "msme";
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
    public ProductListingResponse productListing(ProductListingRequest productListingRequest) {
        catalogueValidator.validateProductListingRequest(productListingRequest);
        if (StringUtils.isBlank(productListingRequest.getCategoryId()) && StringUtils.isBlank(productListingRequest.getSlug())) {
            throw new CentralCommerceServiceException("Either categoryId or slug must be provided", HttpStatus.BAD_REQUEST);
        }
        if (StringUtils.isNotBlank(productListingRequest.getCategoryId()) && StringUtils.isNotBlank(productListingRequest.getSlug())) {
            throw new CentralCommerceServiceException("Both categoryId and slug can not be provided together", HttpStatus.BAD_REQUEST);
        }
        log.info("Processing product listing for identifier: {}",
                StringUtils.isNotBlank(productListingRequest.getCategoryId()) ? productListingRequest.getCategoryId() : productListingRequest.getSlug());

        ProductListingCatalogueResponse catalogueResponse = centralCatalogueClient.productListing(productListingRequest);
        ProductListingCatalogueResponse facetsResponse = centralCatalogueClient.productListingFacetsOnly(productListingRequest);
        return catalogueConverter.convertCataloguePLPResponseToPLPResponse(catalogueResponse, facetsResponse, productListingRequest);
    }
  
    @Override
    public ProductBulkResponse fetchProductsByProductMMIDs(Set<String> productMMIDList) {

        if (productMMIDList == null || productMMIDList.isEmpty()) {
            return new ProductBulkResponse(Collections.emptyList(), 0);
        }

        log.info("Calling Central Catalogue Product Bulk API with Product MMID Count={}",
                productMMIDList.size());

        try {
            ProductBulkRequest request =
                    new ProductBulkRequest(productMMIDList, STOREFRONT_MSME, LOCALE_EN_US);
            return centralCatalogueClient.bulkMMIDResponse(request);
        } catch (Exception e) {
            log.error("Central catalogue call failed (client retries already attempted): {}",
                    e.getMessage(), e);
            return new ProductBulkResponse(Collections.emptyList(), 0);
        }
    }
}
