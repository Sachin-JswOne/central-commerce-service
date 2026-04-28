package com.jswone.commerce.core.enums.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import org.springframework.http.HttpStatus;

import java.util.Arrays;

public enum PromptType {
    AI_INVOKE("ai-invoke"),
    BUSINESS_CLASSIFICATION("business-classification");

    private final String value;

    PromptType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PromptType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new CentralCommerceServiceException("Prompt type cannot be null/blank", HttpStatus.BAD_REQUEST);
        }

        return Arrays.stream(values())
                .filter(promptType -> promptType.value.equalsIgnoreCase(value)
                        || promptType.name().equalsIgnoreCase(value.replace('-', '_')))
                .findFirst()
                .orElseThrow(() -> new CentralCommerceServiceException(
                        "Unsupported prompt type: " + value,
                        HttpStatus.BAD_REQUEST
                ));
    }
}
