package com.jswone.commerce.core.model.elastic.shotlink;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortLinkClicksIndex {

    @JsonProperty("id")
    private String id;

    @JsonProperty("short_link_id")
    private Long shortLinkId;

    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime clickedAt;

    @JsonProperty("user_agent")
    private String userAgent;

    @JsonProperty("ip_hash")
    private String ipHash;

    @JsonProperty("referrer")
    private String referrer;

    @JsonProperty("channel")
    private String channel;

    @JsonProperty("clicked_url")
    private String clickedUrl;

    @JsonProperty("target_url")
    private String targetUrl;

}
