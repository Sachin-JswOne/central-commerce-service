package com.jswone.commerce.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jswone.commerce.core.config.AiPromptConfig;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.request.AiInvokeRequest;
import com.jswone.commerce.core.model.response.AiInvokeResponse;
import com.jswone.commerce.core.rest.LlmClient;
import com.jswone.commerce.core.service.AiService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiPromptConfig aiPromptConfig;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    private String promptTemplate;

    /**
     * Loads the prompt template from the classpath resource file on application startup.
     */
    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource(aiPromptConfig.getTemplatePath());
            promptTemplate = resource.getContentAsString(StandardCharsets.UTF_8);
            log.info("Loaded AI prompt template from: {} ({} chars)",
                    aiPromptConfig.getTemplatePath(), promptTemplate.length());
        } catch (IOException e) {
            log.error("Failed to load AI prompt template from: {}", aiPromptConfig.getTemplatePath(), e);
            throw new CentralCommerceServiceException(
                    "Failed to load AI prompt template file: " + aiPromptConfig.getTemplatePath(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @Override
    public AiInvokeResponse invoke(AiInvokeRequest request) {
        log.info("Processing AI invoke request for PAN: {}, GSTIN: {}", request.getPan(), request.getGstin());

        long startTime = System.currentTimeMillis();

        try {
            String resolvedPrompt = resolvePrompt(request);
            log.debug("Resolved prompt length: {} chars", resolvedPrompt.length());

            String resultStr = llmClient.invoke(resolvedPrompt);
            
            Object parsedResult = resultStr;
            try {
                // First parse the outer LLM wrapper {"text": "...", "usage": {...}}
                com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(resultStr);
                
                // Then try to parse the inner 'text' field if it exists, as it should now be JSON
                if (rootNode.has("text")) {
                    String innerText = rootNode.get("text").asText();
                    try {
                        Object innerJson = objectMapper.readValue(innerText, Object.class);
                        // If inner is valid JSON, rebuild response to contain structured inner data + usage
                        java.util.Map<String, Object> finalResponse = new java.util.HashMap<>();
                        finalResponse.put("data", innerJson);
                        if (rootNode.has("usage")) {
                            finalResponse.put("usage", objectMapper.convertValue(rootNode.get("usage"), Object.class));
                        }
                        parsedResult = finalResponse;
                    } catch (Exception e) {
                        // Inner text wasn't valid JSON, fallback to outer parsed object
                        parsedResult = objectMapper.readValue(resultStr, Object.class);
                    }
                } else {
                    parsedResult = objectMapper.readValue(resultStr, Object.class);
                }
            } catch (Exception e) {
                log.warn("Could not parse LLM result as JSON, returning as string", e);
            }

            long latencyMs = System.currentTimeMillis() - startTime;

            return AiInvokeResponse.builder()
                    .result(parsedResult)
                    .model(aiPromptConfig.getModel())
                    .latencyMs(latencyMs)
                    .build();

        } catch (CentralCommerceServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing AI invoke request: {}", e.getMessage(), e);
            throw new CentralCommerceServiceException(
                    "Failed to process AI invoke request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Replaces placeholders in the prompt template with actual values from the request.
     * Supported placeholders: {{PAN}}, {{GSTIN}}, {{COMPANY_NAME}}, {{LOCATION}}
     */
    private String resolvePrompt(AiInvokeRequest request) {
        if (promptTemplate == null || promptTemplate.isBlank()) {
            throw new CentralCommerceServiceException(
                    "AI prompt template is not loaded. Check 'ai.prompt.template-path' configuration.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        return promptTemplate
                .replace("{{PAN}}", request.getPan())
                .replace("{{GSTIN}}", request.getGstin())
                .replace("{{COMPANY_NAME}}", request.getCompanyName())
                .replace("{{LOCATION}}", request.getLocation());
    }
}
