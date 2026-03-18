package com.jswone.commerce.core.rest.impl;

import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.exceptions.CentralCatalogueServiceException;
import com.jswone.commerce.core.model.*;
import com.jswone.commerce.core.model.centralCatalogue.MetaData;
import com.jswone.commerce.core.model.centralCatalogue.ProductMedia;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.request.FilterRequestProvider;
import com.jswone.commerce.core.model.request.ProductBulkRequest;
import com.jswone.commerce.core.model.request.ProductListingRequest;
import com.jswone.commerce.core.model.request.ProductTypeBulkRequest;
import com.jswone.commerce.core.model.request.Search.SearchRequest;
import com.jswone.commerce.core.model.request.centralCatalogue.CentralCatalogueProductListingRequest;
import com.jswone.commerce.core.model.request.centralCatalogue.CentralCatalogueSearchRequest;
import com.jswone.commerce.core.model.request.centralCatalogue.ProductSlugRequestDTO;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductBulkResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductListingCatalogueResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductSearchResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductTypeBulkResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.DistrictListResponse;
import com.jswone.commerce.core.model.centralCatalogue.Product;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.service.LocationMasterService;
import com.jswone.commerce.core.util.RestUtil;
import com.jswone.commerce.core.util.RetryUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static co.elastic.clients.util.ContentType.APPLICATION_JSON;
import static com.jswone.commerce.core.constants.GenericConstants.*;
import static com.jswone.commerce.core.constants.RestConstants.CLIENT_ID;
import static com.jswone.commerce.core.constants.RestConstants.X_API_KEY;
import static com.jswone.commerce.core.util.CatalogueUtil.extractErrorMessage;
import static com.jswone.commerce.core.util.CatalogueUtil.formatSeoLocationNameToUpperCase;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;

@Service
@Slf4j
public class CentralCatalogueClientImpl implements CentralCatalogueClient {

        private final RestUtil restUtil;
        private final CommerceValueConfig commerceValueConfig;
        private final LocationMasterService locationMasterService;
        private final RetryTemplate catalogueRetryTemplate;

        // Dedicated thread pool for parallel pagination fetching
        private final ExecutorService paginationExecutor = Executors.newFixedThreadPool(10, new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);

                @Override
                public Thread newThread(@NotNull Runnable r) {
                        Thread thread = new Thread(r, "pagination-worker-" + threadNumber.getAndIncrement());
                        thread.setDaemon(true);
                        return thread;
                }
        });

        public CentralCatalogueClientImpl(RestUtil restUtil, CommerceValueConfig commerceValueConfig,
                        @Lazy LocationMasterService locationMasterService,
                        RetryTemplate catalogueRetryTemplate) {
                this.restUtil = restUtil;
                this.commerceValueConfig = commerceValueConfig;
                this.locationMasterService = locationMasterService;
                this.catalogueRetryTemplate = catalogueRetryTemplate;
        }

        @Override
        public ProductSearchResponse genericSearch(SearchRequest searchRequest) {
                try {

                        CentralCatalogueSearchRequest ccRequest = CentralCatalogueSearchRequest.builder()
                                        .query(searchRequest.getText())
                                        .page(searchRequest.getOffSet())
                                        .size(searchRequest.getLimit())
                                        .storefront(searchRequest.getStorefront())
                                        .locale("en-US")
                                        .facets_only(false)
                                        .filters(extractFilters(searchRequest, EMPTY)) // method below
                                        .build();

                        String url = commerceValueConfig.getCentralCatalogueBaseUrl()
                                        + commerceValueConfig.getCentralCatalogueGenericSearchEndpoint();

                        Map<String, String> headers = Map.of(
                                        X_API_KEY, commerceValueConfig.getCentralCatalogueApiKey(),
                                        CLIENT_ID, commerceValueConfig.getCentralCatalogueClientId(),
                                        CONTENT_TYPE, APPLICATION_JSON);

                        log.info("Calling Central Catalogue Search POST API: {}", url);

                        ResponseEntity<ProductSearchResponse> response = RetryUtil.retryHttpCalls(
                                        () -> restUtil.makeRestCall(
                                                        url,
                                                        ccRequest,
                                                        HttpMethod.POST,
                                                        ProductSearchResponse.class,
                                                        headers),
                                        0,
                                        3,
                                        100, CENTRAL_CATALOGUE_SEARCH);

                        return response.getBody();

                } catch (HttpClientErrorException httpClientErrorException) {
                        log.error("HttpClientErrorException while calling central catalogue search: {}",
                                        httpClientErrorException.getMessage(), httpClientErrorException);

                        throw new CentralCatalogueServiceException(
                                        "HttpClientErrorException while calling central catalogue search: "
                                                        + httpClientErrorException.getMessage(),
                                        HttpStatus.valueOf(httpClientErrorException.getStatusCode().value()));
                }
        }

        @Override
        public ProductSearchResponse genericSearchFacetsOnly(SearchRequest searchRequest) {
                try {

                        CentralCatalogueSearchRequest ccRequest = CentralCatalogueSearchRequest.builder()
                                        .query(searchRequest.getText())
                                        .storefront(searchRequest.getStorefront())
                                        .locale("en-US")
                                        .facets_only(true)
                                        .build();

                        String url = commerceValueConfig.getCentralCatalogueBaseUrl()
                                        + commerceValueConfig.getCentralCatalogueGenericSearchEndpoint();

                        Map<String, String> headers = Map.of(
                                        X_API_KEY, commerceValueConfig.getCentralCatalogueApiKey(),
                                        CLIENT_ID, commerceValueConfig.getCentralCatalogueClientId(),
                                        CONTENT_TYPE, APPLICATION_JSON);

                        log.info("Calling Central Catalogue Search facetsOnly POST API: {}", url);

                        ResponseEntity<ProductSearchResponse> response = RetryUtil.retryHttpCalls(
                                        () -> restUtil.makeRestCall(
                                                        url,
                                                        ccRequest,
                                                        HttpMethod.POST,
                                                        ProductSearchResponse.class,
                                                        headers),
                                        0,
                                        3,
                                        100, CENTRAL_CATALOGUE_SEARCH);

                        return response.getBody();

                } catch (HttpClientErrorException httpClientErrorException) {
                        log.error("HttpClientErrorException while calling central catalogue search for facetsOnly: {}",
                                        httpClientErrorException.getMessage(), httpClientErrorException);

                        throw new CentralCatalogueServiceException(
                                        "HttpClientErrorException while calling central catalogue search for facetsOnly: "
                                                        + httpClientErrorException.getMessage(),
                                        HttpStatus.valueOf(httpClientErrorException.getStatusCode().value()));
                }
        }

        private Map<String, List<String>> extractFilters(FilterRequestProvider filterRequestProvider, String location) {

                if (filterRequestProvider.getFilterConditions() == null) {
                        return Collections.emptyMap();
                }

                Map<String, List<String>> filters = new HashMap<>();

                filterRequestProvider.getFilterConditions().forEach(filter -> {

                        if (!"selection".equalsIgnoreCase(filter.getType()))
                                return;

                        List<String> values = filter.getSelectedValues();
                        if (values == null || values.isEmpty())
                                return;

                        filters.put(
                                        filter.getId().toLowerCase(), // GRADE → grade
                                        values);
                });

                addLocationFiltersToRequestFilters(filters, location);
                return filters;
        }

        private void addLocationFiltersToRequestFilters(Map<String, List<String>> filters, String location) {
                if (StringUtils.isNotEmpty(location)) {
                        String formattedName = formatSeoLocationNameToUpperCase(location);

                        // Check if it's a state
                        if (locationMasterService.getAllStates().contains(formattedName)) {
                                filters.put("state", List.of(formattedName));
                        } else {
                                // It's a district (already validated above)
                                filters.put("district", List.of(formattedName));
                        }
                }
        }

        @Override
        public ProductBulkResponse bulkMMIDResponse(ProductBulkRequest productBulkRequest) {
                try {
                        String url = commerceValueConfig.getCentralCatalogueBaseUrl()
                                        + commerceValueConfig.getCentralCatalogueBulkMmidEndpoint();

                        Map<String, String> headers = Map.of(
                                        X_API_KEY, commerceValueConfig.getCentralCatalogueApiKey(),
                                        CLIENT_ID, commerceValueConfig.getCentralCatalogueClientId(),
                                        "Content-Type", "application/json");

                        ResponseEntity<ProductBulkResponse> response = RetryUtil.retryHttpCalls(
                                        () -> restUtil.makeRestCall(
                                                        url,
                                                        productBulkRequest,
                                                        HttpMethod.POST,
                                                        ProductBulkResponse.class,
                                                        headers),
                                        0,
                                        3,
                                        100, CENTRAL_CATALOGUE_BULK_MMID);

                        return response.getBody();

                } catch (HttpClientErrorException httpClientErrorException) {
                        log.error("HttpClientErrorException while calling Central Catalogue bulk MMID API: {}",
                                        httpClientErrorException.getMessage(), httpClientErrorException);

                        throw new CentralCatalogueServiceException(
                                        String.format(
                                                        "HttpClientErrorException while calling Central Catalogue bulk MMID API: %s",
                                                        httpClientErrorException.getMessage()),
                                        HttpStatus.valueOf(httpClientErrorException.getStatusCode().value()));
                }
        }

        public List<CatalogueCategoryTree> getCategoryTree() {
                try {
                        log.info(
                                        "Calling external central catalogue category tree API: {}",
                                        commerceValueConfig.getCatalogueCategoryBaseUrl());

                        Map<String, String> headers = Map.of(
                                        X_API_KEY, commerceValueConfig.getCatalogueCategoryApiKey(),
                                        CLIENT_ID, commerceValueConfig.getCatalogueCategoryClientId());

                        String url = commerceValueConfig.getCatalogueCategoryBaseUrl().concat("/category-tree");

                        ResponseEntity<CatalogueCategoryTreeResponse> response = restUtil.makeRestCall(url, null,
                                        HttpMethod.GET, CatalogueCategoryTreeResponse.class, headers);

                        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                                log.error("Failed to fetch catalogue category tree data: {}", response.getStatusCode());
                                throw new CentralCommerceServiceException(
                                                "Failed to fetch catalogue category tree ",
                                                (HttpStatus) response.getStatusCode());
                        }

                        log.info("Central catalogue tree API call successful");
                        return response.getBody() != null ? response.getBody().getData() : Collections.emptyList();

                } catch (HttpClientErrorException | HttpServerErrorException ex) {
                        log.error("Error calling catalogue tree API: {}", ex.getMessage(), ex);
                        String errorMessage = extractErrorMessage(ex.getResponseBodyAsString());
                        throw new CentralCatalogueServiceException(errorMessage, (HttpStatus) ex.getStatusCode());

                } catch (Exception ex) {
                        log.error("Error calling external Catalogue API: {}", ex.getMessage(), ex);
                        throw new CentralCommerceServiceException(
                                        "Error calling central catalogue category tree API: ",
                                        HttpStatus.INTERNAL_SERVER_ERROR,
                                        ex);
                }
        }

        public CatalogueBreadCrumbData getBreadcrumb(String categoryId, String slug) {
                slug = slug.toLowerCase();
                String url;
                if (Objects.nonNull(categoryId) && Objects.isNull(slug)) {
                        url = commerceValueConfig.getCatalogueCategoryBaseUrl().concat("/category?categoryId=")
                                        .concat(categoryId);
                        log.info("Calling Central Catalogue breadcrumb API via categoryId: {}", url);
                } else {
                        url = commerceValueConfig.getCatalogueCategoryBaseUrl().concat("/category?slug=").concat(slug);
                        log.info("Calling Central Catalogue breadcrumb via slug: {}", url);
                }

                try {

                        Map<String, String> headers = Map.of(
                                        X_API_KEY, commerceValueConfig.getCatalogueCategoryApiKey(),
                                        CLIENT_ID, commerceValueConfig.getCatalogueCategoryClientId());

                        ResponseEntity<CatalogueBreadcrumbResponse> response = restUtil.makeRestCall(url, categoryId,
                                        HttpMethod.GET, CatalogueBreadcrumbResponse.class, headers);

                        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                                log.error("Failed to fetch catalogue breadcrumb data: {}", response.getStatusCode());
                                throw new CentralCommerceServiceException(
                                                "Failed to fetch catalogue breadcrumb data",
                                                (HttpStatus) response.getStatusCode());
                        }

                        log.info("Central catalogue breadcrumb API call successful");
                        return response.getBody() != null ? response.getBody().getData() : null;

                } catch (HttpClientErrorException | HttpServerErrorException ex) {
                        log.error("Error calling catalogue breadcrumb API: {}", ex.getMessage(), ex);
                        String errorMessage = extractErrorMessage(ex.getResponseBodyAsString());
                        throw new CentralCatalogueServiceException(errorMessage, (HttpStatus) ex.getStatusCode());

                } catch (Exception ex) {
                        log.error("Error calling catalogue breadcrumb API: {}", ex.getMessage(), ex);
                        throw new CentralCommerceServiceException(
                                        "Error calling central catalogue breadcrumb API",
                                        HttpStatus.INTERNAL_SERVER_ERROR, ex);
                }
        }

        public ProductTypeBulkResponse bulkTypeIdResponse(ProductTypeBulkRequest productTypeBulkRequest) {
                try {
                        String baseUrl = commerceValueConfig.getCentralCatalogueBaseUrl()
                                        + commerceValueConfig.getCentralCatalogueAdminBulkTypeIdEndpoint();

                        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUrl);

                        if (productTypeBulkRequest.getStorefront() != null) {
                                uriBuilder.queryParam("storefront", productTypeBulkRequest.getStorefront());
                        }

                        // ids=323,447,14,... (comma-separated as required)
                        if (productTypeBulkRequest.getProductTypeIds() != null &&
                                        !productTypeBulkRequest.getProductTypeIds().isEmpty()) {

                                String commaSeparatedIds = String.join(",", productTypeBulkRequest.getProductTypeIds());
                                uriBuilder.queryParam("ids", commaSeparatedIds);
                        }

                        String finalUrl = uriBuilder.toUriString();

                        Map<String, String> headers = Map.of(
                                        X_API_KEY, commerceValueConfig.getCentralCatalogueAdminApiKey(),
                                        CLIENT_ID, commerceValueConfig.getCentralCatalogueAdminClientId(),
                                        "Content-Type", "application/json");

                        ResponseEntity<ProductTypeBulkResponse> response = RetryUtil.retryHttpCalls(
                                        () -> restUtil.makeRestCall(
                                                        finalUrl,
                                                        null,
                                                        HttpMethod.GET,
                                                        ProductTypeBulkResponse.class,
                                                        headers),
                                        0, 3, 100, CENTRAL_CATALOGUE_ADMIN_BULK_TYPEID);

                        return response.getBody();

                } catch (HttpClientErrorException e) {
                        log.error("HttpClientErrorException calling Central Catalogue Admin bulk Product Type Id API: {}",
                                        e.getMessage(), e);

                        throw new CentralCatalogueServiceException(
                                        String.format(
                                                        "HttpClientErrorException calling Central Catalogue Admin bulk Product Type Id API: %s",
                                                        e.getMessage()),
                                        HttpStatus.valueOf(e.getStatusCode().value()));
                }
        }

        public Map<String, ImageMetadata> fetchImagesForMmIds(Set<String> productMmIds) {
                try {
                        log.info("Fetching images for MMIDs: {}", productMmIds);

                        String url = commerceValueConfig.getCentralCatalogueBaseUrl()
                                        + commerceValueConfig.getCentralCatalogueBulkMmidEndpoint();

                        Map<String, String> headers = Map.of(
                                        X_API_KEY, commerceValueConfig.getCentralCatalogueApiKey(),
                                        CLIENT_ID, commerceValueConfig.getCentralCatalogueClientId(),
                                        "Content-Type", "application/json");

                        ProductBulkRequest productBulkRequest = new ProductBulkRequest();
                        productBulkRequest.setProductMMIDS(productMmIds);
                        productBulkRequest.setStorefront("msme");

                        log.info("Calling Central Catalogue bulk MMIDs API with url for images: {}", url);

                        ResponseEntity<ProductBulkResponse> response = RetryUtil.retryHttpCalls(
                                        () -> restUtil.makeRestCall(url, productBulkRequest, HttpMethod.POST,
                                                        ProductBulkResponse.class, headers),
                                        0, 3, 100, CENTRAL_CATALOGUE_SEARCH);

                        ProductBulkResponse productBulkResponse = response.getBody();
                        Map<String, ImageMetadata> imageMap = new HashMap<>();

                        Optional.ofNullable(productBulkResponse)
                                        .map(ProductBulkResponse::getProducts)
                                        .orElse(Collections.emptyList())
                                        .forEach(product -> {
                                                String mmId = product.getProductMmid();

                                                String imageUrl = Optional.ofNullable(product.getMetaData())
                                                                .map(MetaData::getProductMedia)
                                                                .orElse(Collections.emptyList())
                                                                .stream()
                                                                .map(ProductMedia::getPublicUrl)
                                                                .filter(Objects::nonNull)
                                                                .findFirst()
                                                                .orElse(null);

                                                ImageMetadata metadata = new ImageMetadata();
                                                metadata.setImageUrl(imageUrl);
                                                imageMap.put(mmId, metadata);
                                        });

                        log.info("Successfully processed image data for {} MMIDs", imageMap.size());
                        return imageMap;

                } catch (HttpClientErrorException | HttpServerErrorException ex) {
                        log.error("HttpClientErrorException while calling Central Catalogue bulk MMID API: {}",
                                        ex.getMessage(), ex);

                        throw new CentralCatalogueServiceException(
                                        String.format("HttpClientErrorException while calling Central Catalogue bulk MMID API: %s",
                                                        ex.getMessage()),
                                        HttpStatus.valueOf(ex.getStatusCode().value()));
                } catch (Exception ex) {
                        log.error("Exception occurred while calling Central Catalogue bulk MMID API: {}",
                                        ex.getMessage(), ex);
                        throw new CentralCatalogueServiceException(
                                        String.format("Exception occurred while calling Central Catalogue bulk MMID API: %s",
                                                        HttpStatus.INTERNAL_SERVER_ERROR));
                }
        }

        @Override
        public ProductListingCatalogueResponse productListing(ProductListingRequest productListingRequest) {
                try {

                        CentralCatalogueProductListingRequest ccplRequest = CentralCatalogueProductListingRequest
                                        .builder()
                                        .page(productListingRequest.getOffSet())
                                        .size(productListingRequest.getLimit())
                                        .category_id(productListingRequest.getCategoryId())
                                        .slug(productListingRequest.getSlug().toLowerCase())
                                        .storefront(productListingRequest.getStorefront())
                                        .facets_only(false)
                                        .locale("en-US")
                                        .filters(extractFilters(productListingRequest,
                                                        productListingRequest.getLocation()))
                                        .build();

                        String url = commerceValueConfig.getCentralCatalogueBaseUrl()
                                        + commerceValueConfig.getCentralCatalogueProductListingEndpoint();

                        Map<String, String> headers = Map.of(
                                        X_API_KEY, commerceValueConfig.getCentralCatalogueApiKey(),
                                        CLIENT_ID, commerceValueConfig.getCentralCatalogueClientId(),
                                        CONTENT_TYPE, APPLICATION_JSON);

                        log.info("Calling Central Catalogue Product Listing POST API: {}", url);

                        ResponseEntity<ProductListingCatalogueResponse> response = RetryUtil.retryHttpCalls(
                                        () -> restUtil.makeRestCall(
                                                        url,
                                                        ccplRequest,
                                                        HttpMethod.POST,
                                                        ProductListingCatalogueResponse.class,
                                                        headers),
                                        0,
                                        3,
                                        100, CENTRAL_CATALOGUE_SEARCH);

                        return response.getBody();

                } catch (HttpClientErrorException httpClientErrorException) {
                        log.error("HttpClientErrorException while calling central catalogue product listing: {}",
                                        httpClientErrorException.getMessage(), httpClientErrorException);

                        throw new CentralCatalogueServiceException(
                                        "HttpClientErrorException while calling central catalogue product listing: "
                                                        + httpClientErrorException.getMessage(),
                                        HttpStatus.valueOf(httpClientErrorException.getStatusCode().value()));
                }
        }

        @Override
        public ProductListingCatalogueResponse productListingFacetsOnly(ProductListingRequest productListingRequest) {
                try {

                        CentralCatalogueProductListingRequest ccplRequest = CentralCatalogueProductListingRequest
                                        .builder()
                                        .category_id(productListingRequest.getCategoryId())
                                        .slug(productListingRequest.getSlug().toLowerCase())
                                        .storefront(productListingRequest.getStorefront())
                                        .facets_only(true)
                                        .locale("en-US")
                                        .build();

                        String url = commerceValueConfig.getCentralCatalogueBaseUrl()
                                        + commerceValueConfig.getCentralCatalogueProductListingEndpoint();

                        Map<String, String> headers = Map.of(
                                        X_API_KEY, commerceValueConfig.getCentralCatalogueApiKey(),
                                        CLIENT_ID, commerceValueConfig.getCentralCatalogueClientId(),
                                        CONTENT_TYPE, APPLICATION_JSON);

                        log.info("Calling Central Catalogue Product Listing facetsOnly POST API: {}", url);

                        ResponseEntity<ProductListingCatalogueResponse> response = RetryUtil.retryHttpCalls(
                                        () -> restUtil.makeRestCall(
                                                        url,
                                                        ccplRequest,
                                                        HttpMethod.POST,
                                                        ProductListingCatalogueResponse.class,
                                                        headers),
                                        0,
                                        3,
                                        100, CENTRAL_CATALOGUE_SEARCH);

                        return response.getBody();

                } catch (HttpClientErrorException httpClientErrorException) {
                        log.error("HttpClientErrorException while calling central catalogue product listing facetsOnly: {}",
                                        httpClientErrorException.getMessage(), httpClientErrorException);

                        throw new CentralCatalogueServiceException(
                                        "HttpClientErrorException while calling central catalogue product listing facetsOnly: "
                                                        + httpClientErrorException.getMessage(),
                                        HttpStatus.valueOf(httpClientErrorException.getStatusCode().value()));
                }
        }

        @Override
        public ProductBulkResponse getProductFromSlug(String slug, String storeFront, String location) {
                try {

                        ProductSlugRequestDTO productSlugRequestDTO = ProductSlugRequestDTO.builder()
                                        .slug(slug.toLowerCase())
                                        .storefront(storeFront)
                                        .locale("en-US").build();
                        Map<String, List<String>> filters = new HashMap<>();
                        addLocationFiltersToRequestFilters(filters, location);

                        String url = commerceValueConfig.getCentralCatalogueBaseUrl()
                                        + commerceValueConfig.getCentralCatalogueProductSlugEndpoint();

                        Map<String, String> headers = Map.of(
                                        X_API_KEY, commerceValueConfig.getCentralCatalogueApiKey(),
                                        CLIENT_ID, commerceValueConfig.getCentralCatalogueClientId(),
                                        "Content-Type", "application/json");

                        ResponseEntity<ProductBulkResponse> response = RetryUtil.retryHttpCalls(
                                        () -> restUtil.makeRestCall(
                                                        url,
                                                        productSlugRequestDTO,
                                                        HttpMethod.POST,
                                                        ProductBulkResponse.class,
                                                        headers),
                                        0,
                                        3,
                                        100, CENTRAL_CATALOGUE_PRODUCT_SLUG);

                        return response.getBody();

                } catch (HttpClientErrorException httpClientErrorException) {
                        log.error("HttpClientErrorException while calling Central Catalogue Product Slug API: {}",
                                        httpClientErrorException.getMessage(), httpClientErrorException);

                        throw new CentralCatalogueServiceException(
                                        String.format(
                                                        "HttpClientErrorException while calling Central Catalogue Product Slug API: %s",
                                                        httpClientErrorException.getMessage()),
                                        HttpStatus.valueOf(httpClientErrorException.getStatusCode().value()));
                }
        }

        @Override
        public List<Product> getAllProductsForCategoryId(
                        String categoryId,
                        String storefront) {

                int pageSize = 100; // safe page size

                try {
                        // Step 1: Get total count with a small initial request
                        log.info("Fetching product count for category: {}", categoryId);

                        CentralCatalogueProductListingRequest countRequest = CentralCatalogueProductListingRequest
                                        .builder()
                                        .page(0)
                                        .size(1) // Minimal size to get count
                                        .category_id(categoryId)
                                        .storefront(storefront)
                                        .facets_only(true)
                                        .locale("en-US")
                                        .build();

                        String url = commerceValueConfig.getCentralCatalogueBaseUrl()
                                        + commerceValueConfig.getCentralCatalogueProductListingEndpoint();

                        Map<String, String> headers = Map.of(
                                        X_API_KEY, commerceValueConfig.getCentralCatalogueApiKey(),
                                        CLIENT_ID, commerceValueConfig.getCentralCatalogueClientId(),
                                        CONTENT_TYPE, APPLICATION_JSON);

                        ResponseEntity<ProductListingCatalogueResponse> countResponse = RetryUtil.retryHttpCalls(
                                        () -> restUtil.makeRestCall(
                                                        url,
                                                        countRequest,
                                                        HttpMethod.POST,
                                                        ProductListingCatalogueResponse.class,
                                                        headers),
                                        0,
                                        3,
                                        100,
                                        CENTRAL_CATALOGUE_SEARCH);

                        ProductListingCatalogueResponse countBody = countResponse.getBody();
                        if (countBody == null || countBody.getTotalHits() <= 0) {
                                log.info("No products found for category: {}", categoryId);
                                return List.of();
                        }

                        long totalHits = countBody.getTotalHits();
                        int totalPages = (int) Math.ceil((double) totalHits / pageSize);

                        log.info("Category {} has {} products across {} pages - fetching in parallel",
                                        categoryId, totalHits, totalPages);

                        // Step 2: Fetch all pages in parallel
                        Map<Integer, CompletableFuture<List<Product>>> pageFutureMap = new LinkedHashMap<>();

                        for (int page = 0; page < totalPages; page++) {
                                final int currentPage = page;

                                CompletableFuture<List<Product>> pageFuture = CompletableFuture.supplyAsync(
                                                () -> fetchProductPage(categoryId, storefront, currentPage, pageSize,
                                                                url, headers),
                                                paginationExecutor);

                                pageFutureMap.put(page, pageFuture);
                        }

                        // Step 3: Collect results and identify failed pages
                        List<Product> allProducts = new ArrayList<>();
                        List<Integer> failedPages = new ArrayList<>();

                        for (Map.Entry<Integer, CompletableFuture<List<Product>>> entry : pageFutureMap
                                        .entrySet()) {
                                try {
                                        allProducts.addAll(entry.getValue().join());
                                } catch (Exception e) {
                                        log.warn("Page {} failed for category {}: {}",
                                                        entry.getKey(), categoryId, e.getMessage());
                                        failedPages.add(entry.getKey());
                                }
                        }

                        // Step 4: Retry failed pages with exponential backoff
                        if (!failedPages.isEmpty()) {
                                log.info("Retrying {} failed pages for category {} with exponential backoff",
                                                failedPages.size(), categoryId);

                                for (int failedPage : failedPages) {
                                        List<Product> retryResult = retryPageWithBackoff(categoryId, storefront,
                                                        failedPage, pageSize, url, headers);
                                        allProducts.addAll(retryResult);
                                }
                        }

                        log.info("Successfully fetched {} products for category {} (including retries)",
                                        allProducts.size(), categoryId);

                        return allProducts;

                } catch (HttpClientErrorException httpClientErrorException) {

                        log.error(
                                        "HttpClientErrorException while calling central catalogue product listing | categoryId={}",
                                        categoryId,
                                        httpClientErrorException);

                        throw new CentralCatalogueServiceException(
                                        "HttpClientErrorException while calling central catalogue product listing: "
                                                        + httpClientErrorException.getMessage(),
                                        HttpStatus.valueOf(httpClientErrorException.getStatusCode().value()));
                }
        }

        /**
         * Fetch a single page of products for a category
         */
        private List<Product> fetchProductPage(
                        String categoryId,
                        String storefront,
                        int page,
                        int size,
                        String url,
                        Map<String, String> headers) {

                CentralCatalogueProductListingRequest pageRequest = CentralCatalogueProductListingRequest.builder()
                                .page(page)
                                .size(size)
                                .category_id(categoryId)
                                .storefront(storefront)
                                .facets_only(false)
                                .locale("en-US")
                                .build();

                log.debug("Fetching page {} for category: {}", page, categoryId);

                ResponseEntity<ProductListingCatalogueResponse> response = RetryUtil.retryHttpCalls(
                                () -> restUtil.makeRestCall(
                                                url,
                                                pageRequest,
                                                HttpMethod.POST,
                                                ProductListingCatalogueResponse.class,
                                                headers),
                                0,
                                3,
                                100,
                                CENTRAL_CATALOGUE_SEARCH);

                ProductListingCatalogueResponse body = response.getBody();

                if (body == null || body.getProducts() == null) {
                        log.warn("Empty response for page {} of category {}", page, categoryId);
                        return List.of();
                }

                log.debug("Fetched {} products from page {} for category {}",
                                body.getProducts().size(), page, categoryId);

                return body.getProducts();
        }

        /**
         * Retry a failed page fetch using Spring Retry with exponential backoff.
         */
        private List<Product> retryPageWithBackoff(
                        String categoryId,
                        String storefront,
                        int page,
                        int pageSize,
                        String url,
                        Map<String, String> headers) {

                return catalogueRetryTemplate.execute(
                                context -> {
                                        log.info("Retry attempt {}/{} for page {} of category {}",
                                                        context.getRetryCount() + 1, 3, page, categoryId);
                                        return fetchProductPage(categoryId, storefront, page, pageSize, url, headers);
                                },
                                context -> {
                                        log.error("All retry attempts exhausted for page {} of category {}. Skipping page.",
                                                        page, categoryId);
                                        return List.of();
                                });
        }

        public Map<String, String> getStates() {
                try {
                        log.info("Fetching available states from material master service");

                        String url = commerceValueConfig.getCentralCatalogueBaseUrl()
                                        + commerceValueConfig.getCentralCatalogueAdminGetStateEndpoint();

                        Map<String, String> headers = Map.of(
                                        X_API_KEY, commerceValueConfig.getCentralCatalogueAdminApiKey(),
                                        CLIENT_ID, commerceValueConfig.getCentralCatalogueAdminClientId());

                        log.info("Calling Material Master Get State API: {}", url);

                        ResponseEntity<com.jswone.commerce.core.model.response.centralCatalogue.StateListResponse> response = RetryUtil
                                        .retryHttpCalls(
                                                        () -> restUtil.makeRestCall(
                                                                        url,
                                                                        null,
                                                                        HttpMethod.GET,
                                                                        com.jswone.commerce.core.model.response.centralCatalogue.StateListResponse.class,
                                                                        headers),
                                                        0,
                                                        3,
                                                        100,
                                                        "MATERIAL_MASTER_GET_STATE");

                        if (response.getBody() == null || response.getBody().getData() == null) {
                                log.warn("Empty response from get-state API");
                                return Collections.emptyMap();
                        }

                        List<String> states = response.getBody().getData();

                        Map<String, String> stateMap = states.stream()
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toMap(
                                                        state -> state,
                                                        state -> state));

                        log.info("Successfully fetched {} states", stateMap.size());

                        return stateMap;

                } catch (HttpClientErrorException | HttpServerErrorException ex) {
                        log.error("Http error while calling Material Master get-state API: {}",
                                        ex.getMessage(), ex);

                        throw new CentralCatalogueServiceException(
                                        "Error calling Material Master get-state API: " + ex.getMessage(),
                                        HttpStatus.valueOf(ex.getStatusCode().value()));

                } catch (Exception ex) {
                        log.error("Exception occurred while calling Material Master get-state API: {}",
                                        ex.getMessage(), ex);

                        throw new CentralCatalogueServiceException(
                                        "Exception occurred while calling Material Master get-state API",
                                        HttpStatus.INTERNAL_SERVER_ERROR);
                }
        }

        @Override
        public Map<String, Set<String>> getAllServiceableLocations() {
                try {
                        log.info("Fetching all serviceable locations (states and districts)");

                        // Step 1: Get all states
                        Map<String, String> stateMap = getStates();
                        if (stateMap.isEmpty()) {
                                log.warn("No states found, returning empty serviceable locations");
                                return Collections.emptyMap();
                        }

                        // Step 2: Build comma-separated state names for get-district API
                        String stateNamesParam = String.join(",", stateMap.keySet());

                        // Step 3: Call get-district API with all states
                        String url = commerceValueConfig.getCentralCatalogueBaseUrl()
                                        + commerceValueConfig.getCentralCatalogueAdminGetDistrictEndpoint()
                                        + "?state_name=" + stateNamesParam;

                        Map<String, String> headers = Map.of(
                                        X_API_KEY, commerceValueConfig.getCentralCatalogueAdminApiKey(),
                                        CLIENT_ID, commerceValueConfig.getCentralCatalogueAdminClientId());

                        log.info("Calling Material Master Get District API: {}", url);

                        ResponseEntity<DistrictListResponse> response = RetryUtil.retryHttpCalls(
                                        () -> restUtil.makeRestCall(
                                                        url,
                                                        null,
                                                        HttpMethod.GET,
                                                        DistrictListResponse.class,
                                                        headers),
                                        0,
                                        3,
                                        100,
                                        "MATERIAL_MASTER_GET_DISTRICT");

                        if (response.getBody() == null || response.getBody().getData() == null) {
                                log.warn("Empty response from get-district API");
                                return Collections.emptyMap();
                        }

                        // Step 4: Invert Map<District, List<State>> to Map<State, Set<District>>
                        Map<String, List<String>> districtData = response.getBody().getData();
                        Map<String, Set<String>> result = new HashMap<>();

                        // Iterate through each district and its states
                        for (Map.Entry<String, List<String>> entry : districtData.entrySet()) {
                                String district = entry.getKey();
                                List<String> states = entry.getValue();

                                // For each state, add this district to its set
                                for (String state : states) {
                                        result.computeIfAbsent(state, k -> new LinkedHashSet<>()).add(district);
                                }
                        }

                        log.info("Successfully fetched serviceable locations for {} states with {} total districts",
                                        result.size(), districtData.size());

                        return result;

                } catch (HttpClientErrorException | HttpServerErrorException ex) {
                        log.error("Http error while calling Material Master get-district API: {}",
                                        ex.getMessage(), ex);

                        throw new CentralCatalogueServiceException(
                                        "Error calling Material Master get-district API: " + ex.getMessage(),
                                        HttpStatus.valueOf(ex.getStatusCode().value()));

                } catch (Exception ex) {
                        log.error("Exception occurred while calling Material Master get-district API: {}",
                                        ex.getMessage(), ex);

                        throw new CentralCatalogueServiceException(
                                        "Exception occurred while calling Material Master get-district API",
                                        HttpStatus.INTERNAL_SERVER_ERROR);
                }
        }

        @Override
        public SearchedCategoryTree getSearchedCategoryTree(Set<String> categoryIds) {
        try {
                        log.info(
                                "Calling external central catalogue to getSearchedCategoryTree: {}",
                                commerceValueConfig.getCatalogueCategoryBaseUrl());

                        Map<String, String> headers = Map.of(
                                X_API_KEY, commerceValueConfig.getCatalogueCategoryApiKey(),
                                CLIENT_ID, commerceValueConfig.getCatalogueCategoryClientId(),
                                "Content-Type", "application/json");

                String url = UriComponentsBuilder
                        .fromHttpUrl(commerceValueConfig.getCatalogueCategoryBaseUrl())
                        .path("/category-tree")
                        .queryParam("rootCategoryKeys", commerceValueConfig.getCatalogueAdminFilter())
                        .toUriString();

                        ResponseEntity<SearchedCategoryTreeResponse> response = restUtil.makeRestCall(url, new ArrayList<>(categoryIds),
                                HttpMethod.POST, SearchedCategoryTreeResponse.class, headers);

                        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                                log.error("Failed to fetch catalogue category getSearchedCategoryTree: {}", response.getStatusCode());
                                throw new CentralCommerceServiceException(
                                        "Failed to fetch catalogue category getSearchedCategoryTree ",
                                        (HttpStatus) response.getStatusCode());
                        }

                        log.info("Central catalogue getSearchedCategoryTree API call successful");
                        return response.getBody() != null ? response.getBody().getData() : new SearchedCategoryTree();

                } catch (HttpClientErrorException | HttpServerErrorException ex) {
                        log.error("Error calling catalogue getSearchedCategoryTree API: {}", ex.getMessage(), ex);
                        String errorMessage = extractErrorMessage(ex.getResponseBodyAsString());
                        throw new CentralCatalogueServiceException(errorMessage, (HttpStatus) ex.getStatusCode());

                } catch (Exception ex) {
                        log.error("Error calling external Catalogue to getSearchedCategoryTree API: {}", ex.getMessage(), ex);
                        throw new CentralCommerceServiceException(
                                "Error calling central catalogue category getSearchedCategoryTree API: ",
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                ex);
                }
        }
}
