package com.jswone.commerce.core.model.elastic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Builder(toBuilder = true)
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResult {

    @JsonProperty("product_mmid")
    private String productMmid;

    @JsonProperty("product_slug")
    private String productSlug;

    @JsonProperty("product_title")
    private String productTitle;

    @JsonProperty("product_image_url")
    private String productImageUrl;


}
