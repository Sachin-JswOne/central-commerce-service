package com.jswone.commerce.core.model.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JwtUserContext {
    private String sfCustomerId;
    private String sfUserId;
    private String userId;
    private String userType;
    private String storeKey;
    
    @JsonProperty("x-correlation-id")
    private String xCorrelationId;
    
    private Map<String, String> permissions;
    private boolean anonymous;
}
