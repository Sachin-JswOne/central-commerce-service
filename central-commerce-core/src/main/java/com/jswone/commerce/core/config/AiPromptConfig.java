package com.jswone.commerce.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.prompt")
public class AiPromptConfig {

    /**
     * Classpath location of the prompt template file (e.g., prompts/ai-invoke-template.txt).
     * The template supports placeholders: {{PAN}}, {{GSTIN}}, {{COMPANY_NAME}}, {{LOCATION}}
     */
    private String templatePath;

    /**
     * Classpath location of the business classification prompt template file.
     * The template supports placeholders: {{PAN}}, {{GSTIN}}, {{COMPANY_NAME}},
     * {{LOCATION}}, {{BUSINESS_DESCRIPTION}}
     */
    private String businessClassificationTemplatePath;

    /**
     * The AI model to use for invocation (e.g., gemini-2.5-flash).
     */
    private String model;

    /**
     * LLM Playground base URL.
     */
    private String baseUrl;

    /**
     * LLM Playground invoke endpoint path.
     */
    private String invokeEndpoint;

    /**
     * API key for authenticating with the LLM Playground.
     */
    private String apiKey;

    /**
     * User ID for the LLM Playground API.
     */
    private String userId;

    /**
     * Maximum tokens for the AI response.
     */
    private int maxTokens;

    /**
     * Temperature for the AI model (controls randomness).
     */
    private double temperature;
}
