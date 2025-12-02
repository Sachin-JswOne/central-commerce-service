package com.jswone.commerce.core.model.centralCatalogue;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuantityCard {
    @JsonProperty("product_type_id")
    private int productTypeId;
    private Uom uom;
    @JsonProperty("storefront_id")
    private String storefrontId;
    private String label;
    private double rank;
}
