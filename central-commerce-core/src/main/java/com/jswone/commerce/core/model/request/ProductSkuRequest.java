package com.jswone.commerce.core.model.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSkuRequest {
    private String productKey;
    private List<ProductAttributeDTO> productAttributes;
}
