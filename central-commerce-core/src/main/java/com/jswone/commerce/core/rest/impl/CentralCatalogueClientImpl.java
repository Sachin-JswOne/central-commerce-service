package com.jswone.commerce.core.rest.impl;

import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.exceptions.CentralCatalogueServiceException;
import com.jswone.commerce.core.model.request.ProductBulkRequest;
import com.jswone.commerce.core.model.request.Search.SearchRequest;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.jswone.commerce.core.constants.RestConstants.CLIENT_ID;
import static com.jswone.commerce.core.constants.RestConstants.X_API_KEY;
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

            // Base URL
            StringBuilder urlBuilder = new StringBuilder(
                    String.format(
                            "%s%s?query=%s&storefront=%s&size=%d&page=%d",
                            commerceValueConfig.getCentralCatalogueBaseUrl(),
                            commerceValueConfig.getCentralCatalogueGenericSearchEndpoint(),
                            encode(searchRequest.getText()),
                            encode(searchRequest.getStorefront()),
                            searchRequest.getLimit(),
                            searchRequest.getOffSet()
                    )
            );

            // ================================
            //   ADD FILTER CONDITIONS
            // ================================
            if (searchRequest.getFilterConditions() != null) {

                searchRequest.getFilterConditions().forEach(filter -> {

                    // Central Catalogue supports **only selection filters**
                    if (!"selection".equalsIgnoreCase(filter.getType())) return;

                    String key = filter.getId().toLowerCase(); // Example: GRADE -> grade

                    if (filter.getSelectedValues() != null) {
                        filter.getSelectedValues().forEach(selectedValue -> {
                            if (selectedValue != null && !selectedValue.isBlank()) {

                                urlBuilder.append("&")
                                        .append(key)
                                        .append("=")
                                        .append(encode(selectedValue));
                            }
                        });
                    }
                });
            }

            String finalUrl = urlBuilder.toString();

            log.info("Calling Central Catalogue Search URL: {}", finalUrl);

            Map<String, String> headers = Map.of(
                    X_API_KEY, commerceValueConfig.getCentralCatalogueApiKey(),
                    CLIENT_ID, commerceValueConfig.getCentralCatalogueClientId()
            );

            ResponseEntity<ProductSearchResponse> response = RetryUtil.retryHttpCalls( () -> restUtil.makeRestCall(
                    finalUrl,
                    null,
                    HttpMethod.GET,
                    ProductSearchResponse.class,
                    headers
            ),0,
                    3,
                    100);

            return response.getBody();

        } catch (HttpClientErrorException httpClientErrorException) {
            log.error("HttpClientErrorException while calling central catalogue generic search: {}",
                    httpClientErrorException.getMessage(), httpClientErrorException);

            throw new CentralCatalogueServiceException(
                    "HttpClientErrorException while calling central catalogue generic search: "
                            + httpClientErrorException.getMessage(),
                    HttpStatus.valueOf(httpClientErrorException.getStatusCode().value())
            );
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
                    100);

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


    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
