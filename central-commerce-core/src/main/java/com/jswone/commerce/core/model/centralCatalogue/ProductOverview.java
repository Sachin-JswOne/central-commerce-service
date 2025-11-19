package com.jswone.commerce.core.model.centralCatalogue;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductOverview {

  @JsonProperty("supply_condition")
  private String supplyCondition;

  @JsonProperty("product_information")
  private String productInformation;

  @JsonProperty("packaging_information")
  private String packagingInformation;
}
