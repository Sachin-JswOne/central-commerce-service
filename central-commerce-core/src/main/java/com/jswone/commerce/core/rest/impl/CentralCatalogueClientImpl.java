package com.jswone.commerce.core.rest.impl;

import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.exceptions.CentralCatalogueServiceException;
import com.jswone.commerce.core.model.ImageMetadata;
import com.jswone.commerce.core.model.centralCatalogue.MetaData;
import com.jswone.commerce.core.model.centralCatalogue.ProductMedia;
import com.jswone.commerce.core.model.request.ProductBulkRequest;
import com.jswone.commerce.core.model.request.Search.SearchRequest;
import com.jswone.commerce.core.model.request.centralCatalogue.CentralCatalogueSearchRequest;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductBulkResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductSearchResponse;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.util.RestUtil;
import com.jswone.commerce.core.util.RetryUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.jswone.commerce.core.constants.GenericConstants.CENTRAL_CATALOGUE_SEARCH;
import static com.jswone.commerce.core.constants.RestConstants.CLIENT_ID;
import static com.jswone.commerce.core.constants.RestConstants.X_API_KEY;
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
                    100,CENTRAL_CATALOGUE_SEARCH
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

    private Map<String, List<String>> extractFilters(SearchRequest searchRequest) {

        if (searchRequest.getFilterConditions() == null) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> filters = new HashMap<>();

        searchRequest.getFilterConditions().forEach(filter -> {

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

            ResponseEntity<ProductBulkResponse> response = RetryUtil.retryHttpCalls( () ->restUtil.makeRestCall(
                    url,
                    productBulkRequest,
                    HttpMethod.POST,
                    ProductBulkResponse.class,
                    headers
            ),0,
                    3,
                    100,CENTRAL_CATALOGUE_SEARCH);

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

    public Map<String, ImageMetadata> fetchImagesForMmIds(List<String> productMmIds) {
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

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
