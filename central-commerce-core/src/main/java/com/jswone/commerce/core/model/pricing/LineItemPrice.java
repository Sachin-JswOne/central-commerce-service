package com.jswone.commerce.core.model.pricing;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jswone.commerce.core.model.Attribute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LineItemPrice {
    private boolean success;
    private String displayErrorMessage;
    private String errorMessage;
    private ChannelDataDto data;
    private LineItemPriceDto price;
    private double minMoq;
    private Set<Attribute> customAttributes;
    private String switchQuantity;
}
