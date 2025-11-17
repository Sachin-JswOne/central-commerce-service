package com.jswone.commerce.core.model.centralCatalogue;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributes {

    @JsonProperty("product_id")
    private String productId;

    private String grade;

    @JsonProperty("created_at")
    private String createdAt;

    private Integer id;

    @JsonProperty("created_by")
    private String createdBy;

    private String brand;

    @JsonProperty("product_title")
    private String productTitle;

    @JsonProperty("sub_grade")
    private String subGrade;

    @JsonProperty("diameter_min")
    private Double diameterMin;

    @JsonProperty("text_set")
    private List<String> textSet;

    @JsonProperty("localised_text")
    private String localisedText;

    @JsonProperty("meta_title")
    private String metaTitle;

    @JsonProperty("number_set")
    private List<Integer> numberSet;

    @JsonProperty("localized_date")
    private String localizedDate;

    @JsonProperty("boolean_attribute")
    private Boolean booleanAttribute;

    private Integer number;

    @JsonProperty("meta_description")
    private String metaDescription;

    @JsonProperty("diametermm")
    private Integer diameterMM;

    @JsonProperty("rich_text")
    private String richText;

    private String text;

    private String slug;
}
