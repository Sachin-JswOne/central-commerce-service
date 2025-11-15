package com.jswone.commerce.core.model.response;

import com.jswone.commerce.core.entity.catalogue.Attribute;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
