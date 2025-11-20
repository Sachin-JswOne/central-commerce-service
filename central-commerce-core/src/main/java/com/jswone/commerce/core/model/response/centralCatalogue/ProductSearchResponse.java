package com.jswone.commerce.core.model.response.centralCatalogue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jswone.commerce.core.model.centralCatalogue.Product;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchResponse {
  private List<Product> products;

  @JsonProperty("total_hits")
  private long totalHits;
}
