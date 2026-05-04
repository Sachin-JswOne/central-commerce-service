package com.jswone.commerce.core.rest.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.exceptions.AccountMasterException;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.auth.AccountMasterResponse;
import com.jswone.commerce.core.model.auth.PageResponseDTO;
import com.jswone.commerce.core.model.auth.ResourceResponse;
import com.jswone.commerce.core.rest.AccountMasterClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AccountMasterClientImpl implements AccountMasterClient {
    private final RestTemplate jswRestTemplate;

    private final CommerceValueConfig commerceValueConfig;

    private final ObjectMapper objectMapper;

    private static final String ACCOUNT_MASTER_EXP_MSG = "Client Exception occurred while calling the Account Master details.";

    public AccountMasterClientImpl(RestTemplate jswRestTemplate, CommerceValueConfig commerceValueConfig, ObjectMapper objectMapper) {
        this.jswRestTemplate = jswRestTemplate;
        this.commerceValueConfig = commerceValueConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String,String> getAdminPermissionMap(){
        PageResponseDTO<ResourceResponse> response =  getAllResources();
        Set<String> resources = response.getContent().stream().map(ResourceResponse::getName).collect(Collectors.toSet());
        String adminPermissionBits = "11111100";
        Map<String, String> adminPermissionMap =  new HashMap<>();
        resources.forEach(s -> adminPermissionMap.put(s,adminPermissionBits));
        return adminPermissionMap;
    }

    @Override
    public PageResponseDTO<ResourceResponse> getAllResources() {
        try {
            HttpHeaders headers = new HttpHeaders();
            String token = commerceValueConfig.getTemporalToken();
            if(Objects.isNull(token)){
                throw new AccountMasterException("Token is not present");
            }
            headers.add("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);
            headers.add("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String urlTemplate = UriComponentsBuilder.fromHttpUrl(commerceValueConfig.getTemporalBaseUrl())
                    .path("/jswone/resources/v1/get-all")
                    .queryParam("page", 0)
                    .queryParam("size", 100)
                    .encode()
                    .toUriString();

            ResponseEntity<AccountMasterResponse<PageResponseDTO<ResourceResponse>>> response = this.jswRestTemplate.exchange(
                    urlTemplate,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            return handleResponse(response, "getAllResources()");
        } catch (HttpServerErrorException | HttpClientErrorException e) {
            log.error(ACCOUNT_MASTER_EXP_MSG, e);
            throw handleHttpException(e, "getAllResources");
        } catch (Exception exception) {
            log.error("Exception occurred while calling the Account Master details.", exception);
            throw new AccountMasterException("Internal error occurred while calling the Account Master details.", exception);
        }
    }

    private <T> T handleResponse(ResponseEntity<AccountMasterResponse<T>> responseEntity, String methodName) {
        AccountMasterResponse<T> response = responseEntity.getBody();
        if (response == null) {
            log.error("Empty response received from Account Master for method: {}", methodName);
            throw new CentralCommerceServiceException("Empty response received from Account Master", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (Boolean.FALSE.equals(response.getSuccess()) || response.getError() != null) {
            log.error("Account Master returned error for method: {}. Error: {}", methodName, response.getError());
            HttpStatus status = (response.getError() != null && response.getError().getCode() != null)
                    ? response.getError().getCode()
                    : HttpStatus.valueOf(responseEntity.getStatusCode().value());
            String message = (response.getError() != null && response.getError().getMessage() != null)
                    ? response.getError().getMessage()
                    : response.getMessage();
            throw new CentralCommerceServiceException(message, status);
        }

        log.info("Exiting {} with success response.", methodName);
        return response.getData();
    }

    private <T> AccountMasterException handleHttpException(HttpStatusCodeException e, String methodName) {
        try {
            String responseBody = e.getResponseBodyAsString();
            log.error("Error response from Account Master for {}: {}", methodName, responseBody);
            if (!responseBody.isEmpty()) {
                AccountMasterResponse<T> errorResponse = objectMapper.readValue(responseBody,
                        new TypeReference<AccountMasterResponse<T>>() {
                        });
                if (errorResponse != null && errorResponse.getError() != null) {
                    String errorMessage = errorResponse.getError().getMessage();
                    HttpStatus status = errorResponse.getError().getCode() != null
                            ? HttpStatus.valueOf(errorResponse.getError().getCode().value())
                            : HttpStatus.valueOf(e.getStatusCode().value());
                    return new AccountMasterException(errorMessage != null ? errorMessage : ACCOUNT_MASTER_EXP_MSG, e, status);
                }
            }

        } catch (Exception parsingException) {
            log.error("Failed to parse Account Master error response for {}", methodName, parsingException);
        }

        // fallback
        return new AccountMasterException(ACCOUNT_MASTER_EXP_MSG, HttpStatus.valueOf(e.getStatusCode().value()));

    }
}
