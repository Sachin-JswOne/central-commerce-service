package com.jswone.commerce.core.model.masters;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@lombok.Data
public class Data {
    @JsonProperty("parent")
    private Object parent;

    @JsonProperty("product")
    private Product product;

    @JsonProperty("customerServices")
    private Object customerServices;

    @JsonProperty("mmid")
    private String mmid;

    @JsonProperty("variant")
    private Variant variant;

    @JsonProperty("category")
    private Category category;

    @JsonProperty("materialServices")
    private Object materialServices;
}
