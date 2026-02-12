package com.jswone.commerce.core.rest.impl;

import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.exceptions.CentralCatalogueServiceException;
import com.jswone.commerce.core.model.ImageMetadata;
import com.jswone.commerce.core.model.centralCatalogue.MetaData;
import com.jswone.commerce.core.model.centralCatalogue.ProductMedia;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.CatalogueBreadCrumbData;
import com.jswone.commerce.core.model.CatalogueBreadcrumbResponse;
import com.jswone.commerce.core.model.CatalogueCategoryTree;
import com.jswone.commerce.core.model.CatalogueCategoryTreeResponse;
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
import com.jswone.commerce.core.model.centralCatalogue.Product;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.util.RestUtil;
import com.jswone.commerce.core.util.RetryUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.HttpServerErrorException;

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

        // Dedicated thread pool for parallel pagination fetching
        private final ExecutorService paginationExecutor = Executors.newFixedThreadPool(10,
                new ThreadFactory() {
                        private final AtomicInteger threadNumber = new AtomicInteger(1);

                        @Override
                        public Thread newThread(Runnable r) {
                                Thread thread = new Thread(r, "pagination-worker-" + threadNumber.getAndIncrement());
                                thread.setDaemon(true);
                                return thread;
                        }
                });

        public CentralCatalogueClientImpl(RestUtil restUtil, CommerceValueConfig commerceValueConfig) {
                this.restUtil = restUtil;
                this.commerceValueConfig = commerceValueConfig;
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
                    .filters(extractFilters(searchRequest,EMPTY))  // method below
                    .build();

            String url = commerceValueConfig.getCentralCatalogueBaseUrl()
                    + commerceValueConfig.getCentralCatalogueGenericSearchEndpoint();

            Map<String, String> headers = Map.of(
                    X_API_KEY, commerceValueConfig.getCentralCatalogueApiKey(),
                    CLIENT_ID, commerceValueConfig.getCentralCatalogueClientId(),
                    CONTENT_TYPE, APPLICATION_JSON
            );

            log.info("Calling Central Catalogue Search POST API: {}", url);

            ResponseEntity<ProductSearchResponse> response = RetryUtil.retryHttpCalls(
                    () -> restUtil.makeRestCall(
                            url,
                            ccRequest,
                            HttpMethod.POST,
                            ProductSearchResponse.class,
                            headers
                    ),
                    0,
                    3,
                    100, CENTRAL_CATALOGUE_SEARCH
            );

            return response.getBody();

        } catch (HttpClientErrorException httpClientErrorException) {
            log.error("HttpClientErrorException while calling central catalogue search: {}",
                    httpClientErrorException.getMessage(), httpClientErrorException);

            throw new CentralCatalogueServiceException(
                    "HttpClientErrorException while calling central catalogue search: "
                            + httpClientErrorException.getMessage(),
                    HttpStatus.valueOf(httpClientErrorException.getStatusCode().value())
            );
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
                    CONTENT_TYPE, APPLICATION_JSON
            );

            log.info("Calling Central Catalogue Search facetsOnly POST API: {}", url);

            ResponseEntity<ProductSearchResponse> response = RetryUtil.retryHttpCalls(
                    () -> restUtil.makeRestCall(
                            url,
                            ccRequest,
                            HttpMethod.POST,
                            ProductSearchResponse.class,
                            headers
                    ),
                    0,
                    3,
                    100, CENTRAL_CATALOGUE_SEARCH
            );

            return response.getBody();

        } catch (HttpClientErrorException httpClientErrorException) {
            log.error("HttpClientErrorException while calling central catalogue search for facetsOnly: {}",
                    httpClientErrorException.getMessage(), httpClientErrorException);

            throw new CentralCatalogueServiceException(
                    "HttpClientErrorException while calling central catalogue search for facetsOnly: "
                            + httpClientErrorException.getMessage(),
                    HttpStatus.valueOf(httpClientErrorException.getStatusCode().value())
            );
        }
    }

    private Map<String, List<String>> extractFilters(FilterRequestProvider filterRequestProvider, String location) {

        if (filterRequestProvider.getFilterConditions() == null) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> filters = new HashMap<>();

        filterRequestProvider.getFilterConditions().forEach(filter -> {

            if (!"selection".equalsIgnoreCase(filter.getType())) return;

            List<String> values = filter.getSelectedValues();
            if (values == null || values.isEmpty()) return;

            filters.put(
                    filter.getId().toLowerCase(),  // GRADE → grade
                    values
            );
        });

        //Adding location filter to filter products based on location
        if(StringUtils.isNotEmpty(location)) {
                Map<String,String> states = getStates();
                String formattedName = formatSeoLocationNameToUpperCase(location);
                if(states.containsKey(formattedName)) {
                        filters.put("state",List.of(formattedName));
                }else{
                        filters.put("district",List.of(formattedName));
                }
        }

        return filters;
    }

    @Override
    public ProductBulkResponse bulkMMIDResponse(ProductBulkRequest productBulkRequest) {
        try {
            String url = commerceValueConfig.getCentralCatalogueBaseUrl()
                    + commerceValueConfig.getCentralCatalogueBulkMmidEndpoint();

            Map<String, String> headers = Map.of(
                    X_API_KEY, commerceValueConfig.getCentralCatalogueApiKey(),
                    CLIENT_ID, commerceValueConfig.getCentralCatalogueClientId(),
                    "Content-Type", "application/json"
            );

            ResponseEntity<ProductBulkResponse> response = RetryUtil.retryHttpCalls(() -> restUtil.makeRestCall(
                            url,
                            productBulkRequest,
                            HttpMethod.POST,
                            ProductBulkResponse.class,
                            headers
                    ), 0,
                    3,
                    100, CENTRAL_CATALOGUE_BULK_MMID);

            return response.getBody();

        } catch (HttpClientErrorException httpClientErrorException) {
            log.error("HttpClientErrorException while calling Central Catalogue bulk MMID API: {}",
                    httpClientErrorException.getMessage(), httpClientErrorException);

            throw new CentralCatalogueServiceException(
                    String.format(
                            "HttpClientErrorException while calling Central Catalogue bulk MMID API: %s",
                            httpClientErrorException.getMessage()
                    ),
                    HttpStatus.valueOf(httpClientErrorException.getStatusCode().value())
            );
        }
    }

    public List<CatalogueCategoryTree> getCategoryTree() {
        try {
            log.info(
                    "Calling external central catalogue category tree API: {}",
                    commerceValueConfig.getCatalogueCategoryBaseUrl());

            Map<String, String> headers = Map.of(
                    X_API_KEY, commerceValueConfig.getCatalogueCategoryApiKey(),
                    CLIENT_ID, commerceValueConfig.getCatalogueCategoryClientId()
            );

            String url = commerceValueConfig.getCatalogueCategoryBaseUrl().concat("/category-tree");

            ResponseEntity<CatalogueCategoryTreeResponse> response =
                    restUtil.makeRestCall(url, null, HttpMethod.GET, CatalogueCategoryTreeResponse.class, headers);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.error("Failed to fetch catalogue category tree data: {}", response.getStatusCode());
                throw new CentralCommerceServiceException(
                        "Failed to fetch catalogue category tree ", (HttpStatus) response.getStatusCode());
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
        String url;
        if(Objects.nonNull(categoryId) && Objects.isNull(slug)){
            url = commerceValueConfig.getCatalogueCategoryBaseUrl().concat("/category?categoryId=").concat(categoryId);
            log.info("Calling Central Catalogue breadcrumb API via categoryId: {}", url);
        }else {
            url = commerceValueConfig.getCatalogueCategoryBaseUrl().concat("/category?slug=").concat(slug);
            log.info("Calling Central Catalogue breadcrumb via slug: {}", url);
        }


        try {

            Map<String, String> headers = Map.of(
                    X_API_KEY, commerceValueConfig.getCatalogueCategoryApiKey(),
                    CLIENT_ID, commerceValueConfig.getCatalogueCategoryClientId()
            );

            ResponseEntity<CatalogueBreadcrumbResponse> response =
                    restUtil.makeRestCall(url, categoryId, HttpMethod.GET, CatalogueBreadcrumbResponse.class, headers);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.error("Failed to fetch catalogue breadcrumb data: {}", response.getStatusCode());
                throw new CentralCommerceServiceException(
                        "Failed to fetch catalogue breadcrumb data", (HttpStatus) response.getStatusCode());
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
                    "Error calling central catalogue breadcrumb API", HttpStatus.INTERNAL_SERVER_ERROR, ex);
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

            // ids=323,447,14,...  (comma-separated as required)
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

            ResponseEntity<ProductTypeBulkResponse> response =
                    RetryUtil.retryHttpCalls(
                            () -> restUtil.makeRestCall(
                                    finalUrl,
                                    null,
                                    HttpMethod.GET,
                                    ProductTypeBulkResponse.class,
                                    headers), 0, 3, 100, CENTRAL_CATALOGUE_ADMIN_BULK_TYPEID);

            return response.getBody();

        } catch (HttpClientErrorException e) {
            log.error("HttpClientErrorException calling Central Catalogue Admin bulk Product Type Id API: {}",
                    e.getMessage(), e);

            throw new CentralCatalogueServiceException(
                    String.format(
                            "HttpClientErrorException calling Central Catalogue Admin bulk Product Type Id API: %s",
                            e.getMessage()
                    ),
                    HttpStatus.valueOf(e.getStatusCode().value())
            );
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
                    "Content-Type", "application/json"
            );

            ProductBulkRequest productBulkRequest = new ProductBulkRequest();
            productBulkRequest.setProductMMIDS(productMmIds);
            productBulkRequest.setStorefront("msme");

            log.info("Calling Central Catalogue bulk MMIDs API with url for images: {}", url);

            ResponseEntity<ProductBulkResponse> response = RetryUtil.retryHttpCalls(
                    () -> restUtil.makeRestCall(url, productBulkRequest, HttpMethod.POST,
                            ProductBulkResponse.class, headers),
                    0, 3, 100, CENTRAL_CATALOGUE_SEARCH
            );

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
                    HttpStatus.valueOf(ex.getStatusCode().value())
            );
        } catch (Exception ex) {
            log.error("Exception occurred while calling Central Catalogue bulk MMID API: {}", ex.getMessage(), ex);
            throw new CentralCatalogueServiceException(
                    String.format("Exception occurred while calling Central Catalogue bulk MMID API: %s",
                            HttpStatus.INTERNAL_SERVER_ERROR)
            );
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
                                        .slug(productListingRequest.getSlug())
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
                    CONTENT_TYPE, APPLICATION_JSON
            );

            log.info("Calling Central Catalogue Product Listing POST API: {}", url);

            ResponseEntity<ProductListingCatalogueResponse> response = RetryUtil.retryHttpCalls(
                    () -> restUtil.makeRestCall(
                            url,
                            ccplRequest,
                            HttpMethod.POST,
                            ProductListingCatalogueResponse.class,
                            headers
                    ),
                    0,
                    3,
                    100,CENTRAL_CATALOGUE_SEARCH
            );

            return response.getBody();

        } catch (HttpClientErrorException httpClientErrorException) {
            log.error("HttpClientErrorException while calling central catalogue product listing: {}",
                    httpClientErrorException.getMessage(), httpClientErrorException);

            throw new CentralCatalogueServiceException(
                    "HttpClientErrorException while calling central catalogue product listing: "
                            + httpClientErrorException.getMessage(),
                    HttpStatus.valueOf(httpClientErrorException.getStatusCode().value())
            );
        }
    }

    @Override
    public ProductListingCatalogueResponse productListingFacetsOnly(ProductListingRequest productListingRequest) {
        try {

            CentralCatalogueProductListingRequest ccplRequest = CentralCatalogueProductListingRequest.builder()
                    .category_id(productListingRequest.getCategoryId())
                    .slug(productListingRequest.getSlug())
                    .storefront(productListingRequest.getStorefront())
                    .facets_only(true)
                    .locale("en-US")
                    .build();

            String url = commerceValueConfig.getCentralCatalogueBaseUrl()
                    + commerceValueConfig.getCentralCatalogueProductListingEndpoint();

            Map<String, String> headers = Map.of(
                    X_API_KEY, commerceValueConfig.getCentralCatalogueApiKey(),
                    CLIENT_ID, commerceValueConfig.getCentralCatalogueClientId(),
                    CONTENT_TYPE, APPLICATION_JSON
            );

            log.info("Calling Central Catalogue Product Listing facetsOnly POST API: {}", url);

            ResponseEntity<ProductListingCatalogueResponse> response = RetryUtil.retryHttpCalls(
                    () -> restUtil.makeRestCall(
                            url,
                            ccplRequest,
                            HttpMethod.POST,
                            ProductListingCatalogueResponse.class,
                            headers
                    ),
                    0,
                    3,
                    100,CENTRAL_CATALOGUE_SEARCH
            );

            return response.getBody();

        } catch (HttpClientErrorException httpClientErrorException) {
            log.error("HttpClientErrorException while calling central catalogue product listing facetsOnly: {}",
                    httpClientErrorException.getMessage(), httpClientErrorException);

            throw new CentralCatalogueServiceException(
                    "HttpClientErrorException while calling central catalogue product listing facetsOnly: "
                            + httpClientErrorException.getMessage(),
                    HttpStatus.valueOf(httpClientErrorException.getStatusCode().value())
            );
        }
    }

    @Override
    public ProductBulkResponse getProductFromSlug(String slug, String storeFront) {
        try {
            ProductSlugRequestDTO productSlugRequestDTO = ProductSlugRequestDTO.builder()
                    .slug(slug)
                    .storefront(storeFront)
                    .locale("en-US").build();

            String url = commerceValueConfig.getCentralCatalogueBaseUrl()
                    + commerceValueConfig.getCentralCatalogueProductSlugEndpoint();

            Map<String, String> headers = Map.of(
                    X_API_KEY, commerceValueConfig.getCentralCatalogueApiKey(),
                    CLIENT_ID, commerceValueConfig.getCentralCatalogueClientId(),
                    "Content-Type", "application/json"
            );

            ResponseEntity<ProductBulkResponse> response = RetryUtil.retryHttpCalls(() -> restUtil.makeRestCall(
                            url,
                            productSlugRequestDTO,
                            HttpMethod.POST,
                            ProductBulkResponse.class,
                            headers
                    ), 0,
                    3,
                    100, CENTRAL_CATALOGUE_PRODUCT_SLUG);

            return response.getBody();

        } catch (HttpClientErrorException httpClientErrorException) {
            log.error("HttpClientErrorException while calling Central Catalogue Product Slug API: {}",
                    httpClientErrorException.getMessage(), httpClientErrorException);

            throw new CentralCatalogueServiceException(
                    String.format(
                            "HttpClientErrorException while calling Central Catalogue Product Slug API: %s",
                            httpClientErrorException.getMessage()
                    ),
                    HttpStatus.valueOf(httpClientErrorException.getStatusCode().value())
            );
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
                        List<CompletableFuture<List<Product>>> pageFutures = new ArrayList<>();

                        for (int page = 0; page < totalPages; page++) {
                                final int currentPage = page;

                                CompletableFuture<List<Product>> pageFuture = CompletableFuture.supplyAsync(() -> {
                                        try {
                                                return fetchProductPage(categoryId, storefront, currentPage, pageSize,
                                                                url,
                                                                headers);
                                        } catch (Exception e) {
                                                log.error("Error fetching page {} for category {}: {}",
                                                                currentPage, categoryId, e.getMessage(), e);
                                                return List.of();
                                        }
                                }, paginationExecutor);

                                pageFutures.add(pageFuture);
                        }

                        // Step 3: Wait for all pages and merge results
                        List<Product> allProducts = pageFutures.stream()
                                .map(CompletableFuture::join)
                                .flatMap(List::stream)
                                .collect(Collectors.toList());

                        log.info("Successfully fetched {} products for category {} using parallel pagination",
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

        @Override
        public Map<String, String> getStates() {
                try {
                        log.info("Fetching available states from material master service");

                        String url = commerceValueConfig.getCentralCatalogueBaseUrl()
                                + commerceValueConfig.getCentralCatalogueAdminGetStateEndpoint();

                        Map<String, String> headers = Map.of(
                                X_API_KEY, commerceValueConfig.getCentralCatalogueAdminApiKey(),
                                CLIENT_ID, commerceValueConfig.getCentralCatalogueAdminClientId());

                        log.info("Calling Material Master Get State API: {}", url);

                        ResponseEntity<com.jswone.commerce.core.model.response.centralCatalogue.StateListResponse> response =
                                RetryUtil.retryHttpCalls(
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
                                        state -> state
                                ));

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
}
