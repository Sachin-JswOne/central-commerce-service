package com.jswone.commerce.core.model.request.shortlink;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jswone.commerce.core.enums.shortlink.LinkType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateShortLinkRequest {

    @NotNull
    private LinkType type;

    @NotBlank
    @JsonProperty("business_id")
    private String businessId;

    private String channel;

    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;

    // Optional UTM overrides
    @JsonProperty("utm_source")
    private String utmSource;

    @JsonProperty("utm_medium")
    private String utmMedium;

    @JsonProperty("utm_campaign")
    private String utmCampaign;

    @JsonProperty("utm_content")
    private String utmContent;

    @JsonProperty("link")
    private String link;

}
