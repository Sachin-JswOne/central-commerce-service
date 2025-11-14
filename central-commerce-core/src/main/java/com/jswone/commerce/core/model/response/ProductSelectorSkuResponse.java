package com.jswone.commerce.core.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSelectorSkuResponse {
    private String id;
    private String productKey;
    private String productName;
    private List<SkuInfo> matchedSkus;
    private SkuInfo masterSku;
    private List<String> availableSkuKeys;
}
