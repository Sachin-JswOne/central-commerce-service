package com.jswone.commerce.core.model.response;

import com.jswone.commerce.core.entity.catalogue.Attribute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuInfo {
    private String variantKey;
    private List<Attribute> customAttribute;
    private String variantMMID;
    private String productTypeKey;
    private String sku;
    private String productKey;
}
