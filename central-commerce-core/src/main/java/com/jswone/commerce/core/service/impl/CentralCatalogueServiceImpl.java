package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.converters.CatalogueConverter;
import com.jswone.commerce.core.enums.seo.SeoEntityType;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.CategoryTreeResponse;
import com.jswone.commerce.core.model.ImageMetadata;
import com.jswone.commerce.core.model.NavigationItem;
import com.jswone.commerce.core.model.centralCatalogue.AssociatedCategory;
import com.jswone.commerce.core.model.request.BulkCategoryRequestDTO;
import com.jswone.commerce.core.model.request.ProductBulkRequest;
import com.jswone.commerce.core.model.request.ProductListingRequest;
import com.jswone.commerce.core.model.request.Search.SearchRequest;
import com.jswone.commerce.core.model.response.ProductListingResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductBulkResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductListingCatalogueResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductSearchResponse;
import com.jswone.commerce.core.model.response.plp.ProductFilterConditions;
import com.jswone.commerce.core.model.response.search.SearchResponse;
import com.jswone.commerce.core.model.seo.SeoContext;
import com.jswone.commerce.core.publisher.recentSearch.UserSearchLogsItemPublisher;
import com.jswone.commerce.core.resolver.SeoContextResolver;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.service.CatalogueCategoryService;
import com.jswone.commerce.core.service.CentralCatalogueService;
import com.jswone.commerce.core.util.CatalogueUtil;
import com.jswone.commerce.core.validators.CatalogueValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.jswone.commerce.core.constants.BuyAgainConstants.LOCALE_EN_US;
import static com.jswone.commerce.core.constants.GenericConstants.BULK_IMAGE_CHUNK_SIZE;

@Service
@Slf4j
public class CentralCatalogueServiceImpl implements CentralCatalogueService {

    public static final String STOREFRONT_MSME = "msme";
    private final CentralCatalogueClient centralCatalogueClient;
    private final CatalogueConverter catalogueConverter;
    private final CatalogueValidator catalogueValidator;
    private final UserSearchLogsItemPublisher userSearchLogsItemPublisher;
    private final SeoContextResolver seoContextResolver;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final CatalogueCategoryService categoryService;

    public CentralCatalogueServiceImpl(
            CentralCatalogueClient centralCatalogueClient,
            CatalogueConverter catalogueConverter,
            CatalogueValidator catalogueValidator,
            UserSearchLogsItemPublisher userSearchLogsItemPublisher,
            SeoContextResolver seoContextResolver,
            CatalogueCategoryService categoryService
    ) {
        this.centralCatalogueClient = centralCatalogueClient;
        this.catalogueConverter = catalogueConverter;
        this.catalogueValidator = catalogueValidator;
        this.userSearchLogsItemPublisher = userSearchLogsItemPublisher;
        this.seoContextResolver = seoContextResolver;
        this.categoryService = categoryService;
    }

    @Override
    public SearchResponse searchCatalogue(SearchRequest searchRequest) {
        catalogueValidator.validateSearchRequest(searchRequest);
        ProductSearchResponse productSearchResponse = centralCatalogueClient.genericSearch(searchRequest);
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
        if(Objects.nonNull(categoryIds) &&
           !categoryIds.isEmpty() &&
           (Objects.isNull(categoryFilterConditions) ||
           Objects.isNull(categoryFilterConditions.getId()))) {
                categoryTreeResponse = categoryService.getSearchedCatalogueCategoryTree(categoryIds);
            }


        return catalogueConverter.convertGenericSearchToSearchResponse(productSearchResponse, searchRequest, categoryTreeResponse, categoryFilterConditions);
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

    @Override
    public ProductListingResponse productListing(ProductListingRequest productListingRequest) {
        catalogueValidator.validateProductListingRequest(productListingRequest);

        if (StringUtils.isBlank(productListingRequest.getCategoryId())
                && StringUtils.isBlank(productListingRequest.getSlug())) {
            throw new CentralCommerceServiceException("Either categoryId or slug must be provided",
                    HttpStatus.BAD_REQUEST);
        }
        if (StringUtils.isNotBlank(productListingRequest.getCategoryId())
                && StringUtils.isNotBlank(productListingRequest.getSlug())) {
            throw new CentralCommerceServiceException("Both categoryId and slug can not be provided together",
                    HttpStatus.BAD_REQUEST);
        }

        // Build SeoContext from slug and location in request body
        if (StringUtils.isNotBlank(productListingRequest.getSlug())) {
            // Build URL path from slug and location to resolve SeoContext
            String urlPath = buildUrlPath(productListingRequest.getSlug(), productListingRequest.getLocation());
            SeoContext seoContext = seoContextResolver.resolve(urlPath, SeoEntityType.CATEGORY);

            log.info("Resolved SeoContext for product listing: entityType={}, pageType={}, slug={}, location={}",
                    seoContext.getEntityType(), seoContext.getPageType(), seoContext.getSlug(),
                    seoContext.getLocation());

            // You can now use seoContext for additional metadata or validation
            // For example, you might want to enrich the request or log additional context
        }

        log.info("Processing product listing for identifier: {}",
                StringUtils.isNotBlank(productListingRequest.getCategoryId()) ? productListingRequest.getCategoryId()
                        : productListingRequest.getSlug());

        ProductListingCatalogueResponse catalogueResponse = centralCatalogueClient.productListing(productListingRequest);
        CategoryTreeResponse categoryTreeResponse = null;
        ProductFilterConditions categoryFilterConditions =
                Optional.ofNullable(productListingRequest.getFilterConditions())
                        .orElse(Collections.emptyList())
                        .stream()
                        .filter(fc -> fc.getId().equalsIgnoreCase("CATEGORY"))
                        .filter(fc -> fc.getSelectedValues() != null && !fc.getSelectedValues().isEmpty())
                        .findAny()
                        .orElse(null);

        if(Objects.isNull(categoryFilterConditions) || Objects.isNull(categoryFilterConditions.getId())){
            if (Objects.nonNull(productListingRequest.getSlug())) {
                categoryTreeResponse = categoryService.getBulkCatalogueCategoryTree(
                        BulkCategoryRequestDTO
                                .builder()
                                .categorySlugs(List.of(productListingRequest.getSlug()))
                                .build());
            } else {
                categoryTreeResponse = categoryService.getBulkCatalogueCategoryTree(
                        BulkCategoryRequestDTO
                                .builder()
                                .categoryIds(List.of(productListingRequest.getCategoryId()))
                                .build());
            }
            List<NavigationItem> navigationItems = catalogueConverter.buildFilteredMenu(catalogueResponse,categoryTreeResponse);
            categoryTreeResponse.setNavigation(navigationItems);
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
    public Map<String, ProductListingResponse> productListingBulk(
            List<String> slugs) {

        List<CompletableFuture<ProductListingResponse>> futures =
                slugs.stream()
                        .map(
                                slug ->
                                        CompletableFuture.supplyAsync(
                                                        () -> {
                                                            ProductListingRequest request =
                                                                    new ProductListingRequest();
                                                            request.setSlug(slug);
                                                            return productListing(request);
                                                        },
                                                        executorService)
                                                .handle(
                                                        (result, ex) -> {
                                                            if (ex != null) {
                                                                log.error(
                                                                        "Error occurred while fetching products for slug: {}",
                                                                        slug,
                                                                        ex);
                                                                result = new ProductListingResponse();
                                                            }
                                                            result.setCategoryId(slug);
                                                            return result; // recover and continue
                                                        }))
                        .toList();
        // Wait for all tasks to complete (same as reference)
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(
                        Collectors.toMap(
                                ProductListingResponse::getCategoryId,
                                Function.identity()));
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
