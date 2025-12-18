package com.jswone.commerce.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogueBreadcrumbResponse {
  private int statusCode;
  private String status;
  private CatalogueBreadCrumbData data;
  private ErrorResponse error;
}
