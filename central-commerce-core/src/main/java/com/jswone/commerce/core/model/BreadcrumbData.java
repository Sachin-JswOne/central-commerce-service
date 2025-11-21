package com.jswone.commerce.core.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreadcrumbData {
  private String urlKey;
  private String breadCrumb;
  private String description;
  private List<BreadcrumbDetail> breadCrumbDetailList;
}
