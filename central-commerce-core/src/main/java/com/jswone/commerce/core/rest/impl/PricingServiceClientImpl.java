package com.jswone.commerce.core.rest.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.rest.PricingServiceClient;
import com.jswone.commerce.core.util.RestUtil;
import com.jswone.commerce.core.util.RetryUtil;
import com.jswone.commons.pricing.PricingServiceResponse;
import com.jswone.commons.pricing.ProductPricingRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import static com.jswone.commerce.core.constants.GenericConstants.CENTRAL_CATALOGUE_PRICING;
import static com.jswone.commerce.core.constants.RestConstants.X_API_KEY;

@Service
@Slf4j
public class PricingServiceClientImpl implements PricingServiceClient {
    private final RestUtil restUtil;
    private final CommerceValueConfig commerceValueConfig;
    private final ObjectMapper objectMapper;

    public PricingServiceClientImpl(RestUtil restUtil, CommerceValueConfig commerceValueConfig, ObjectMapper objectMapper) {
        this.restUtil = restUtil;
        this.commerceValueConfig = commerceValueConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    public PricingServiceResponse callPricingService(ProductPricingRequest productPricingRequest) {
        try {
            log.info(
                    "Pricing Request for PDP : {}",
                    objectMapper.writeValueAsString(productPricingRequest));

            Map<String, String> headers = Map.of(
                    X_API_KEY, commerceValueConfig.getPricingXApiKey(),
                    "Content-Type", "application/json"
            );

            String url = commerceValueConfig.getPricingServiceBaseUrl()
                    + commerceValueConfig.getPriceFetchEndpoint();

            Instant start = Instant.now();

            ResponseEntity<PricingServiceResponse> response =
                    RetryUtil.retryHttpCalls(
                            () ->
                                    restUtil.makeRestCall(
                                            url,
                                            productPricingRequest,
                                            HttpMethod.POST,
                                            PricingServiceResponse.class,
                                            headers),
                            0,
                            3,
                            100,
                            CENTRAL_CATALOGUE_PRICING);

            Instant end = Instant.now();
            Duration timeElapsed = Duration.between(start, end);

            log.info(
                    "Pricing Response for PDP : {}",
                    objectMapper.writeValueAsString(response.getBody()));
            log.info(
                    "Time taken for the pricing service call: {} milliseconds with referenceId:{}",
                    timeElapsed.toMillis(),
                    Objects.requireNonNull(response.getBody()).getReferenceId());
            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new CentralCommerceServiceException(
                    "HttpClientErrorException occurred while calling the Pricing Master| Exception"
                            + " Message:"
                            + ex.getMessage(),
                    HttpStatus.valueOf(ex.getStatusCode().value()),
                    ex);

        } catch (Exception ex) {
            throw new CentralCommerceServiceException(
                    "Exception occurred while calling the Pricing Master| Exception Message:"
                            + ex.getMessage(),
                    ex);
        }
    }
}
