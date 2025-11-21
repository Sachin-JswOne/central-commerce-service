package com.jswone.commerce.core.model.response.plp;

import com.google.cloud.spring.data.datastore.core.mapping.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
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
