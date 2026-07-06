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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private static final String ADMIN_PERMISSION_BITS = "11111100";
    private static final int PAGE_SIZE = 100;

    public AccountMasterClientImpl(RestTemplate jswRestTemplate, CommerceValueConfig commerceValueConfig, ObjectMapper objectMapper) {
        this.jswRestTemplate = jswRestTemplate;
        this.commerceValueConfig = commerceValueConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String,String> getAdminPermissionMap(){
        List<ResourceResponse> allResources = new ArrayList<>();
        int page = 0;
        PageResponseDTO<ResourceResponse> response;
        do {
            response = fetchResourcePage(page++, PAGE_SIZE);
            if (response.getContent() != null) {
                allResources.addAll(response.getContent());
            }
        } while (!response.isLast());

        Map<String, String> adminPermissionMap = new HashMap<>();
        allResources.stream()
                .map(ResourceResponse::getName)
                .forEach(name -> adminPermissionMap.put(name, ADMIN_PERMISSION_BITS));
        return adminPermissionMap;
    }

    @Override
    public PageResponseDTO<ResourceResponse> getAllResources() {
        return fetchResourcePage(0, PAGE_SIZE);
    }

    private PageResponseDTO<ResourceResponse> fetchResourcePage(int page, int size) {
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
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .encode()
                    .toUriString();

            ResponseEntity<AccountMasterResponse<PageResponseDTO<ResourceResponse>>> response = this.jswRestTemplate.exchange(
                    urlTemplate,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            return handleResponse(response, "fetchResourcePage(page=" + page + ")");
        } catch (HttpServerErrorException | HttpClientErrorException e) {
            log.error(ACCOUNT_MASTER_EXP_MSG, e);
            throw handleHttpException(e, "fetchResourcePage");
        } catch (AccountMasterException e) {
            throw e;
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
