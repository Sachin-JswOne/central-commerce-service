package com.jswone.commerce.core.model.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProductSkuRequest {
    private String productMaterialMasterId;
    private List<ProductAttributeDTO> productAttributes;
}
