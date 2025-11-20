package com.jswone.commerce.core.model.response.centralCatalogue;

import com.jswone.commerce.core.model.centralCatalogue.Product;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductBulkResponse {
  private List<Product> products;
  private int totalHits;
}
