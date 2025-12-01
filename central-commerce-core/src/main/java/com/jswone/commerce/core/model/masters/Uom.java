package com.jswone.commerce.core.model.masters;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Uom {
    @JsonProperty("quantityPrecision")
    private int quantityPrecision;

    @JsonProperty("categoryUomId")
    private int categoryUomId;

    @JsonProperty("uiLabelPrice")
    private String uiLabelPrice;

    @JsonProperty("uomClass")
    private String uomClass;

    @JsonProperty("uomType")
    private String uomType;

    @JsonProperty("uiLabelQuantity")
    private String uiLabelQuantity;

    @JsonProperty("name")
    private String name;

    @JsonProperty("categoryKey")
    private int categoryKey;

    @JsonProperty("uomId")
    private int uomId;

    @JsonProperty("categoryId")
    private int categoryId;
}
