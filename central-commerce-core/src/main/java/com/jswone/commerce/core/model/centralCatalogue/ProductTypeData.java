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
public class ProductTypeData {

    private int id;
    private String category_mid;
    @JsonProperty("variant_selectors")
    private Map<String, VariantSelector> variantSelectors;
    @JsonProperty("quantity_cards")
    private List<QuantityCard> quantityCards;
    @JsonProperty("standard_attributes")
    private List<Map<String, Object>> standardAttributes;
    private List<Map<String, Object>> attributes;
}
