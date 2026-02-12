package com.jswone.commerce.core.model;

import com.jswone.commerce.core.model.seo.SeoMeta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogueBreadcrumbAttributes {
  private String slug;
  private String seo_url;
  private String meta_title;
  private String category_title;
  private String category_description;
  private String category_detail_description;
  private String category_content_heading;
  private String category_content_description;
  private String category_faqs_heading;
  private String category_faqs_description;
  private CatalogueMetaImage meta_image;
  private SeoMeta seo_meta;
}
