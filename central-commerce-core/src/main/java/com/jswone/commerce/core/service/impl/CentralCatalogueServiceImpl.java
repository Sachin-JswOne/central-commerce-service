package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.converters.CatalogueConverter;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.CategoryTreeResponse;
import com.jswone.commerce.core.model.request.ProductListingRequest;
import com.jswone.commerce.core.model.request.Search.SearchRequest;
import com.jswone.commerce.core.model.response.ProductListingResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductListingCatalogueResponse;
import com.jswone.commerce.core.model.request.ProductBulkRequest;
import com.jswone.commerce.core.model.request.Search.SearchRequest;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductBulkResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductSearchResponse;
import com.jswone.commerce.core.model.response.plp.ProductFilterConditions;
import com.jswone.commerce.core.model.response.search.SearchResponse;
import com.jswone.commerce.core.publisher.recentSearch.UserSearchLogsItemPublisher;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.service.CatalogueCategoryService;
import com.jswone.commerce.core.service.CentralCatalogueService;
import com.jswone.commerce.core.validators.CatalogueValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.jswone.commerce.core.constants.BuyAgainConstants.LOCALE_EN_US;

@Service
@Slf4j
public class CentralCatalogueServiceImpl implements CentralCatalogueService {

    private static final String STOREFRONT_MSME = "msme";
    private final CentralCatalogueClient centralCatalogueClient;
    private final CatalogueConverter catalogueConverter;
    private final CatalogueValidator catalogueValidator;
    private final UserSearchLogsItemPublisher userSearchLogsItemPublisher;
    private final CatalogueCategoryService categoryService;

    public CentralCatalogueServiceImpl(CentralCatalogueClient centralCatalogueClient,
                                       CatalogueConverter catalogueConverter,
                                       CatalogueValidator catalogueValidator,
                                       UserSearchLogsItemPublisher userSearchLogsItemPublisher,
                                       CatalogueCategoryService categoryService) {
        this.centralCatalogueClient = centralCatalogueClient;
        this.catalogueConverter = catalogueConverter;
        this.catalogueValidator = catalogueValidator;
        this.userSearchLogsItemPublisher = userSearchLogsItemPublisher;
        this.categoryService = categoryService;
    }

    @Override
    public SearchResponse searchCatalogue(SearchRequest searchRequest) {
        catalogueValidator.validateSearchRequest(searchRequest);
        ProductSearchResponse productSearchResponse = centralCatalogueClient.genericSearch(searchRequest);
        if(productSearchResponse.getProducts().isEmpty() && Objects.nonNull(searchRequest.getFilterConditions())){
            SearchResponse searchResponse = new SearchResponse();
            searchResponse.setFilterConditions(searchRequest.getFilterConditions());
            return searchResponse;
        }
        userSearchLogsItemPublisher.publish(productSearchResponse, searchRequest);
        CategoryTreeResponse categoryTreeResponse = null;
        ProductFilterConditions categoryFilterConditions =
                Optional.ofNullable(searchRequest.getFilterConditions())
                        .orElse(Collections.emptyList())
                        .stream()
                        .filter(fc -> fc.getId().equalsIgnoreCase("CATEGORY"))
                        .filter(fc -> fc.getSelectedValues() != null && !fc.getSelectedValues().isEmpty())
                        .findAny()
                        .orElse(null);
        Set<String> categoryIds = productSearchResponse.getFacets().get("category_id");

        boolean hasCategoryIds = categoryIds != null && !categoryIds.isEmpty();
        boolean isFilterMissing = categoryFilterConditions == null || categoryFilterConditions.getId() == null;
        if (hasCategoryIds && isFilterMissing) {
            categoryTreeResponse = categoryService.getSearchedCatalogueCategoryTree(
                    categoryIds);
        }

        return catalogueConverter.convertGenericSearchToSearchResponse(productSearchResponse, searchRequest, categoryTreeResponse, categoryFilterConditions);
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
        if(catalogueResponse.getProducts().isEmpty() && Objects.nonNull(productListingRequest.getFilterConditions())){
            ProductListingResponse productListingResponse = new ProductListingResponse();
            productListingResponse.setFilterConditions(productListingRequest.getFilterConditions());
         return productListingResponse;
        }
        CategoryTreeResponse categoryTreeResponse = null;
        ProductFilterConditions categoryFilterConditions =
                Optional.ofNullable(productListingRequest.getFilterConditions())
                        .orElse(Collections.emptyList())
                        .stream()
                        .filter(fc -> fc.getId().equalsIgnoreCase("CATEGORY"))
                        .filter(fc -> fc.getSelectedValues() != null && !fc.getSelectedValues().isEmpty())
                        .findAny()
                        .orElse(null);
        Set<String> categoryIds = catalogueResponse.getFacets().get("category_id");

        boolean hasCategoryIds = categoryIds != null && !categoryIds.isEmpty();
        boolean isFilterMissing = categoryFilterConditions == null || categoryFilterConditions.getId() == null;
        if (hasCategoryIds && isFilterMissing) {
            categoryTreeResponse = categoryService.getSearchedCatalogueCategoryTree(
                    categoryIds);
        }
        return catalogueConverter.convertCataloguePLPResponseToPLPResponse(catalogueResponse, productListingRequest, categoryTreeResponse, categoryFilterConditions);
    }

    /**
     * Build URL path from slug and location for SeoContext resolution
     * Uses /category/ pattern for product listing pages
     */
    private String buildUrlPath(String slug, String location) {
        if (StringUtils.isNotBlank(location)) {
            // If location is provided, build full URL: /category/{location}/{slug}
            return String.format("/%s/%s", location, slug);
        } else {
            // If no location, just use slug: /category/{slug}
            return String.format("/%s", slug);
        }
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
