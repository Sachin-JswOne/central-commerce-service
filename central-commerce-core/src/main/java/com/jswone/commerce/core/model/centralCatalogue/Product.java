package com.jswone.commerce.core.model.centralCatalogue;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

  private String id;

  private int version;

  private ProductAttributes attributes;

  private List<Variant> variants;

  @JsonProperty("product_type_id")
  private String productTypeId;

  @JsonProperty("product_mmid")
  private String productMmid;

  @JsonProperty("associated_categories")
  private List<AssociatedCategory> associatedCategories;

  @JsonProperty("created_at")
  private String createdAt;

  @JsonProperty("last_modified_at")
  private String lastModifiedAt;

  @JsonProperty("enabled_storefronts")
  private List<String> enabledStorefronts;

  @JsonProperty("meta_data")
  private MetaData metaData;

  private Double rank;

  @JsonProperty("product_location")
  private List<ProductLocation> productLocation;
}
