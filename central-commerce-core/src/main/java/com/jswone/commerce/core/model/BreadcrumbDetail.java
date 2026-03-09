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
public class BreadcrumbDetail {
  private String name;
  private String categoryId;
  private String categoryKey;
  private String slug;
  private String detailDescription;
  private String description;
  private String categoryContentHeading;
  private String categoryContentDescription;
  private String faqsHeading;
  private String faqsDescription;
  private MetaImage metaImage;
  private SeoMeta seoMeta;
  private String metaTitle;
  private String metaDescription;
  private String shortDescription;
}
