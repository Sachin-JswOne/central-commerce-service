package com.jswone.commerce.core.model.centralCatalogue;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Variant {

    @JsonProperty("created_at")
    private long createdAt;

    private Map<String, String> attributes;

    @JsonProperty("variant_mmid")
    private String variantMmid;

    private String id;

    @JsonProperty("enabled_storefront")
    private List<String> enabledStorefront;

    private int version;

    @JsonProperty("meta_image")
    private MetaImageDTO metaImage;
}

