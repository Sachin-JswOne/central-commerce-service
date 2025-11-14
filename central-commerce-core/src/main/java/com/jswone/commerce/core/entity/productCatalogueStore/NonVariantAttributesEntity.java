package com.jswone.commerce.core.entity.productCatalogueStore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NonVariantAttributesEntity {
    private String attributeNameInCt;
    private String displayName;
    private String displayType;
    private List<ProductAttribute> value;
    private String productTypeKey;
}
