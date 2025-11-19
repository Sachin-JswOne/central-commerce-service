package com.jswone.commerce.core.model.centralCatalogue;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssociatedCategoryAttributes {
  private String category_title;

  private String slug;

  @JsonProperty("meta_title")
  private String metaTitle;
}
