package com.jswone.commerce.core.model.masters;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Category {
    @JsonProperty("leaf_category")
    private String leafCategory;
    @JsonProperty("path")
    private String path;
    @JsonProperty("category_key")
    private int categoryKey;
    @JsonProperty("uom")
    private List<Uom> uom;
    @JsonProperty("product_type")
    private String productType;
    @JsonProperty("form")
    private String form;
    @JsonProperty("category_id")
    private int categoryId;
    @JsonProperty("sub_category")
    private String subCategory;
    @JsonProperty("master_category")
    private String masterCategory;
}
