package com.jswone.commerce.core.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jswone.commerce.core.config.AiPromptConfig;
import com.jswone.commerce.core.enums.ai.PromptType;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.request.AiInvokeRequest;
import com.jswone.commerce.core.model.response.AiInvokeResponse;
import com.jswone.commerce.core.rest.LlmClient;
import com.jswone.commerce.core.service.AiService;
import com.jswone.commerce.core.service.PromptManagementService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiPromptConfig aiPromptConfig;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final PromptManagementService promptManagementService;

    private String promptTemplate;
    private String businessClassificationPromptTemplate;

    @PostConstruct
    public void init() {
        try {
            promptTemplate = loadPromptTemplate(aiPromptConfig.getTemplatePath(), "AI prompt");
            businessClassificationPromptTemplate = loadPromptTemplate(
                    aiPromptConfig.getBusinessClassificationTemplatePath(),
                    "AI business classification prompt"
            );
        } catch (IOException e) {
            log.error("Failed to load AI prompt templates", e);
            throw new CentralCommerceServiceException(
                    "Failed to load AI prompt template file",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @Override
    public AiInvokeResponse invoke(AiInvokeRequest request) {
        log.info("Processing AI invoke request for PAN: {}, GSTIN: {}", request.getPan(), request.getGstin());
        return invokeWithTemplate(
                request,
                resolveManagedPrompt(PromptType.AI_INVOKE, promptTemplate, "AI invoke"),
                false,
                "AI invoke"
        );
    }

    @Override
    public AiInvokeResponse invokeBusinessClassification(AiInvokeRequest request) {
        log.info("Processing AI business classification request for PAN: {}, GSTIN: {}",
                request.getPan(), request.getGstin());

        if (request.getBusinessDescription() == null || request.getBusinessDescription().isBlank()) {
            throw new CentralCommerceServiceException(
                    "Business description cannot be null/blank",
                    HttpStatus.BAD_REQUEST
            );
        }

        return invokeWithTemplate(
                request,
                resolveManagedPrompt(
                        PromptType.BUSINESS_CLASSIFICATION,
                        businessClassificationPromptTemplate,
                        "AI business classification"
                ),
                true,
                "AI business classification"
        );
    }

    private String loadPromptTemplate(String templatePath, String templateLabel) throws IOException {
        if (templatePath == null || templatePath.isBlank()) {
            throw new CentralCommerceServiceException(templateLabel + " path is not configured.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ClassPathResource resource = new ClassPathResource(templatePath);
        String template = resource.getContentAsString(StandardCharsets.UTF_8);
        log.info("Loaded {} template from: {} ({} chars)", templateLabel, templatePath, template.length());
        return template;
    }

    private AiInvokeResponse invokeWithTemplate(
            AiInvokeRequest request,
            String template,
            boolean webSearch,
            String operationName
    ) {
        long startTime = System.currentTimeMillis();

        try {
            String resolvedPrompt = resolvePrompt(template, request);
            log.debug("Resolved {} prompt length: {} chars", operationName, resolvedPrompt.length());

            String resultStr = llmClient.invoke(resolvedPrompt, webSearch);
            Object parsedResult = parseResult(resultStr);


            return AiInvokeResponse.builder()
                    .result(parsedResult)
                    .build();
        } catch (CentralCommerceServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing {} request: {}", operationName, e.getMessage(), e);
            throw new CentralCommerceServiceException(
                    "Failed to process " + operationName + " request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private Object parseResult(String resultStr) {
        Object parsedResult = resultStr;

        try {
            JsonNode rootNode = objectMapper.readTree(resultStr);

            if (rootNode.has("text")) {
                String innerText = rootNode.get("text").asText();
                try {
                    Object innerJson = objectMapper.readValue(cleanJsonPayload(innerText), Object.class);
                    Map<String, Object> finalResponse = new HashMap<>();
                    finalResponse.put("data", innerJson);
                    parsedResult = finalResponse;
                } catch (Exception e) {
                    parsedResult = objectMapper.readValue(resultStr, Object.class);
                }
            } else {
                parsedResult = objectMapper.readValue(resultStr, Object.class);
            }
        } catch (Exception e) {
            log.warn("Could not parse LLM result as JSON, returning as string", e);
        }
        return parsedResult;
    }

    private String cleanJsonPayload(String payload) {
        if (payload == null) {
            return null;
        }

        String trimmedPayload = payload.trim();
        if (!trimmedPayload.startsWith("```")) {
            return trimmedPayload;
        }

        int firstLineBreak = trimmedPayload.indexOf('\n');
        if (firstLineBreak < 0) {
            return trimmedPayload;
        }

        String withoutOpeningFence = trimmedPayload.substring(firstLineBreak + 1);
        int closingFenceIndex = withoutOpeningFence.lastIndexOf("```");
        if (closingFenceIndex < 0) {
            return trimmedPayload;
        }

        return withoutOpeningFence.substring(0, closingFenceIndex).trim();
    }

    private String resolvePrompt(String template, AiInvokeRequest request) {
        if (template == null || template.isBlank()) {
            throw new CentralCommerceServiceException(
                    "AI prompt template is not loaded.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        return template
                .replace("{{PAN}}", defaultString(request.getPan()))
                .replace("{{GSTIN}}", defaultString(request.getGstin()))
                .replace("{{COMPANY_NAME}}", defaultString(request.getCompanyName()))
                .replace("{{LOCATION}}", defaultString(request.getLocation()))
                .replace("{{BUSINESS_DESCRIPTION}}", defaultString(request.getBusinessDescription()));
    }

    private String resolveManagedPrompt(PromptType promptType, String fallbackTemplate, String operationName) {
        try {
            return promptManagementService.findActiveDecodedPrompt(promptType)
                    .map(prompt -> {
                        log.debug("Using managed prompt for {}", operationName);
                        return prompt;
                    })
                    .orElseGet(() -> {
                        log.debug("No managed prompt found for {}, falling back to classpath template", operationName);
                        return fallbackTemplate;
                    });
        } catch (Exception exception) {
            log.warn("Managed prompt lookup failed for {}, falling back to classpath template", operationName, exception);
            return fallbackTemplate;
        }
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
