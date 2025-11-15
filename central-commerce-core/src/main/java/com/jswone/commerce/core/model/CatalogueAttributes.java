package com.jswone.commerce.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogueAttributes {
  private String meta_description;
  private String seo_url;
  private String meta_title;
  private String slug;
  private String href;
}
