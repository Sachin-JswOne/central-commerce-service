package com.jswone.commerce.core.model.pricing;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jswone.commons.pricing.PricingMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LineItemPriceDto {
    private Double primaryUomVariantPrice;
    private String displayPrimaryUomVariantPrice;
    private Double ctUomVariantPrice;
    private String displayCtUomVariantPrice;
    private String primaryUom;
    private String primaryUomValue;
    private String displayPrimaryUomValue;
    private String secondaryUom;
    private String secondaryUomValue;
    private String displaySecondaryUomValue;
    private String secondaryUomLabel;
    private String ctUom;
    private String ctUomValue;
    private Double variantLineItemPrice;
    private String primaryUomLabel;
    private String displayVariantLineItemPrice;
    private PartingChargeResponse partingCharge;
    private PricingMode pricingMode;
}
