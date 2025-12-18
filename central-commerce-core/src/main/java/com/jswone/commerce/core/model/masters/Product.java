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
public class Product {

    @JsonProperty("sub_brand")
    private String subBrand;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("product_key")
    private int productKey;

    @JsonProperty("product_id")
    private int productId;

    @JsonProperty("grade")
    private String grade;

    @JsonProperty("modified_by")
    private String modifiedBy;

    @JsonProperty("modified_at")
    private String modifiedAt;

    @JsonProperty("brand")
    private String brand;

    @JsonProperty("hash")
    private String hash;

    @JsonProperty("status")
    private String status;

    @JsonProperty("sub_grade")
    private String subGrade;

}
