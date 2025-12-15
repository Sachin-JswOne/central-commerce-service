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
  private String category_title;
  private String meta_title;
  private String slug;
  private String href;
  private String link_title_seo_purpose;
  private String link_title;
  private CatalogueMetaImage meta_image;
}
