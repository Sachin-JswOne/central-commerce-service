package com.jswone.commerce.core.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductBulkRequest {

    @JsonProperty("product_mmids")
    private Set<String> productMMIDS;

    private String storefront;

    private String locale;
}
