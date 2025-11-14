package com.jswone.commerce.core.entity.productCatalogueStore;

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
public class CartOrderDisplayCardAttributesDto {
    private String displayName;
    @JsonProperty("attributeNameInCt")
    private String attributeNameInCT;
    private String displayType;
    private String measureUnit;
}
