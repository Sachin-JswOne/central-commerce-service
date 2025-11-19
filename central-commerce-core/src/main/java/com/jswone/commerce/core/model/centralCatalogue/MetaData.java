package com.jswone.commerce.core.model.centralCatalogue;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetaData {
  @JsonProperty("product_media")
  private List<ProductMedia> productMedia;

  @JsonProperty("product_overview")
  private ProductOverview productOverview;
}
