package com.jswone.commerce.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogueMetaImage {
  private String title;
  private String alt_text;
  private String asset_id;
  private String file_name;
  private String public_url;
  private String content_type;
}
