package com.jswone.commerce.core.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductBulkRequest {

  @JsonProperty("product_mmids")
  private List<String> productMMIDS;

  private String storefront;

  private String locale;
}
