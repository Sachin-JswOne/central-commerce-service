package com.jswone.commerce.core.entity.productCatalogueStore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JSWPriceRange {
    private String name;
    private double minPrice;
    private String displayMinPrice;
    private double maxPrice;
    private String displayMaxPrice;
    private String currency;
    private String unit;
    private String label;
    private String priceLabel;
}
