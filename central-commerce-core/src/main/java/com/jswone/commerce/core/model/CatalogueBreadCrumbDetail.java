package com.jswone.commerce.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogueBreadCrumbDetail {
  private String id;
  private String key;
  private String parent_id;
  private CatalogueBreadcrumbAttributes attributes;
}
