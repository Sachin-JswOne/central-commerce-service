package com.jswone.commerce.core.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UomValueDetails<T> {
    private String unit;
    private String label;
    private T value;
    private String priceLabel;
}