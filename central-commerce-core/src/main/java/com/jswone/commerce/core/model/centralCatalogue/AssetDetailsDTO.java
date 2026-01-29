package com.jswone.commerce.core.model.centralCatalogue;

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
public class AssetDetailsDTO {
    @JsonProperty("asset_id")
    private String assetId;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("content_type")
    private String contentType;

    @JsonProperty("asset_type")
    private String assetType;

    @JsonProperty("file_size")
    private long fileSize;

    @JsonProperty("public_url")
    private String publicUrl;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

}
