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
public class NavigationItem {
  private String id;
  private String name;
  private String slug;
  private String seoUrl;
  private String metaTitle;
  private String metaDescription;
  private String href;
  private String linkTitleSeoPurpose;
  private String linkTitle;
  private List<NavigationItem> subMenu;
}
