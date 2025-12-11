package com.jswone.commerce.core.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductTypeBulkRequest {

    @JsonProperty("ids")
    private Set<String> productTypeIds;

    private String storefront;

}
