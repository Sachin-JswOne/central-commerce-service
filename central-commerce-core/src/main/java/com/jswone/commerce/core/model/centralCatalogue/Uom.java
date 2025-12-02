package com.jswone.commerce.core.model.centralCatalogue;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Uom {
    private String name;
    @JsonProperty("category_uom_id")
    private int categoryUomId;
    @JsonProperty("uom_id")
    private int uomId;
    @JsonProperty("category_id")
    private int categoryId;
    @JsonProperty("category_key")
    private int categoryKey;
    @JsonProperty("uom_type")
    private String uomType;
    @JsonProperty("uom_class")
    private String uomClass;
    @JsonProperty("ui_label_quantity")
    private String uiLabelQuantity;
    @JsonProperty("ui_label_price")
    private String uiLabelPrice;
    @JsonProperty("quantity_precision")
    private int quantityPrecision;
}
