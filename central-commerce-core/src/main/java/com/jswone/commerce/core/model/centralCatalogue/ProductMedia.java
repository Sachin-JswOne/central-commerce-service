package com.jswone.commerce.core.model.centralCatalogue;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductMedia {

    @JsonProperty("public_url")
    private String publicUrl;

    @JsonProperty("content_type")
    private String contentType;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("asset_type")
    private String assetType;

    @JsonProperty("meta_data")
    private MediaMetaData metaData;

    private int rank;
}

