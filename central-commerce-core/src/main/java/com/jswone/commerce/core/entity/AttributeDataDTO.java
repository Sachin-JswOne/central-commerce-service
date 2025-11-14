package com.jswone.commerce.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttributeDataDTO {
    @JsonProperty("category_id")
    private int categoryId;
    @JsonProperty("name")
    private String name;
    @JsonProperty("optional")
    private boolean optional;
    @JsonProperty("unit")
    private String unit;
    @JsonProperty("value_type")
    private String valueType;
    @JsonProperty("ui_label")
    private String uiLabel;
}
