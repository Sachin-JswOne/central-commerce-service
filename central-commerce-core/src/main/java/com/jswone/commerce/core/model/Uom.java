package com.jswone.commerce.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class Uom {
    private String unit;
    private String label;
    private double value;
    private String priceLabel;
}
