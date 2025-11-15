package com.jswone.commerce.core.model.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProductSkuRequest {
  private String productMaterialMasterId;
  private List<ProductAttributeDTO> productAttributes;
}
