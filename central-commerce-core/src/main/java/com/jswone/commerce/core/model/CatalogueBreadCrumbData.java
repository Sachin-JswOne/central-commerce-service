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
public class CatalogueBreadCrumbData {
  private String url_key;
  private String bread_crumb;
  private String description;
  private List<CatalogueBreadCrumbDetail> bread_crumb_details;
}
