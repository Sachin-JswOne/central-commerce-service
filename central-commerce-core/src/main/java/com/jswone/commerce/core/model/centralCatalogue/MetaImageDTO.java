package com.jswone.commerce.core.model.centralCatalogue;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetaImageDTO {
    private String title;

    @JsonProperty("asset_details")
    private AssetDetailsDTO assetDetails;

    @JsonProperty("alt_text")
    private String altText;
}
