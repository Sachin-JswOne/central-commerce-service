package com.jswone.commerce.core.model;

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
}
