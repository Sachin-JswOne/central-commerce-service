package com.jswone.commerce.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchedCategoryTree {
    @JsonProperty("all_products")
    private List<CatalogueCategoryTree> allProducts;
    @JsonProperty("industry_segments")
    private List<CatalogueCategoryTree> industrySegments;
}
