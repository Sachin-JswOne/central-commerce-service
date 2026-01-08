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
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.util.RestUtil;
import com.jswone.commerce.core.util.RetryUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.HttpServerErrorException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Collections;
import java.util.Objects;

import static com.jswone.commerce.core.constants.GenericConstants.*;
import static com.jswone.commerce.core.constants.RestConstants.CLIENT_ID;
import static com.jswone.commerce.core.constants.RestConstants.X_API_KEY;
import static com.jswone.commerce.core.util.CatalogueUtil.extractErrorMessage;
import static io.grpc.netty.shaded.io.netty.handler.codec.http.HttpHeaders.Values.APPLICATION_JSON;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;

@Service
@Slf4j
public class CentralCatalogueClientImpl implements CentralCatalogueClient {

    private final RestUtil restUtil;
    private final CommerceValueConfig commerceValueConfig;

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
                    .filters(extractFilters(searchRequest))  // method below
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

    private Map<String, List<String>> extractFilters(FilterRequestProvider filterRequestProvider) {

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

            CentralCatalogueProductListingRequest ccplRequest = CentralCatalogueProductListingRequest.builder()
                    .page(productListingRequest.getOffSet())
                    .size(productListingRequest.getLimit())
                    .category_id(productListingRequest.getCategoryId())
                    .slug(productListingRequest.getSlug())
                    .storefront(productListingRequest.getStorefront())
                    .facets_only(false)
                    .locale("en-US")
                    .filters(extractFilters(productListingRequest))
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


    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
