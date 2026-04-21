package com.jswone.commerce.core.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmInvokeRequest {

    private String model;

    @JsonProperty("user_id")
    private String userId;

    private String prompt;

    private boolean stream;

    @JsonProperty("stream_mode")
    private String streamMode;

    private List<Object> attachments;

    @JsonProperty("max_tokens")
    private int maxTokens;

    private double temperature;

    @JsonProperty("web_search")
    private boolean webSearch;
}
