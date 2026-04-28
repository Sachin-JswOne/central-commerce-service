package com.jswone.commerce.core.rest.impl;

import com.jswone.commerce.core.config.AiPromptConfig;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.request.LlmInvokeRequest;
import com.jswone.commerce.core.rest.LlmClient;
import com.jswone.commerce.core.util.RestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.ArrayList;
import java.util.Map;

import static com.jswone.commerce.core.constants.RestConstants.X_API_KEY;

@Service
@Slf4j
public class LlmClientImpl implements LlmClient {

    private final RestUtil restUtil;
    private final AiPromptConfig aiPromptConfig;

    public LlmClientImpl(RestUtil restUtil, AiPromptConfig aiPromptConfig) {
        this.restUtil = restUtil;
        this.aiPromptConfig = aiPromptConfig;
    }

    @Override
    public String invoke(String resolvedPrompt) {
        return invoke(resolvedPrompt, false);
    }

    @Override
    public String invoke(String resolvedPrompt, boolean webSearch) {
        try {
            String url = aiPromptConfig.getBaseUrl() + aiPromptConfig.getInvokeEndpoint();

            Map<String, String> headers = Map.of(
                    X_API_KEY, aiPromptConfig.getApiKey(),
                    "Content-Type", "application/json"
            );

            LlmInvokeRequest llmRequest = LlmInvokeRequest.builder()
                    .model(aiPromptConfig.getModel())
                    .userId(aiPromptConfig.getUserId())
                    .prompt(resolvedPrompt)
                    .stream(true)
                    .streamMode("concat")
                    .attachments(new ArrayList<>())
                    .maxTokens(aiPromptConfig.getMaxTokens())
                    .temperature(aiPromptConfig.getTemperature())
                    .webSearch(webSearch)
                    .build();

            log.info("Calling LLM Playground API: {}", url);

            ResponseEntity<String> response = restUtil.makeRestCall(
                    url,
                    llmRequest,
                    HttpMethod.POST,
                    String.class,
                    headers
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.error("LLM API returned non-OK status: {}", response.getStatusCode());
                throw new CentralCommerceServiceException(
                        "LLM API returned non-OK status: " + response.getStatusCode(),
                        (HttpStatus) response.getStatusCode()
                );
            }

            log.info("LLM Playground API call successful");
            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("HTTP error calling LLM Playground API: {} - {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new CentralCommerceServiceException(
                    "HTTP error calling LLM API: " + ex.getMessage(),
                    HttpStatus.valueOf(ex.getStatusCode().value())
            );
        } catch (CentralCommerceServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error calling LLM Playground API: {}", ex.getMessage(), ex);
            throw new CentralCommerceServiceException(
                    "Error calling LLM Playground API: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ex
            );
        }
    }
}
