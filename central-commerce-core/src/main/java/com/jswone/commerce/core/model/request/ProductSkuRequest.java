package com.jswone.commerce.core.model.request;


import jakarta.validation.constraints.NotNull;
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
    @NotNull(message = "Product Material Master Id cannot be null/blank")
    private String productMaterialMasterId;
    @NotNull(message = "Product Attributes cannot be null/blank")
    private List<ProductAttributeDTO> productAttributes;
}
