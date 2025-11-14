package com.jswone.commerce.core.entity.productCatalogueStore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
public class VariantSelector {
    private String displayName;
    private String displayType;
    private String attributeNameInCT;
    private String minValue;
    private String maxValue;
    private String textboxInputLabel;
}
