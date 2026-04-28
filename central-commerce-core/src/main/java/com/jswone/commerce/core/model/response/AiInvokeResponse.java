package com.jswone.commerce.core.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiInvokeResponse {
    private Object result;

//    private String model;
//
//    private Map<String, Object> metadata;
//
//    private long latencyMs;
}
