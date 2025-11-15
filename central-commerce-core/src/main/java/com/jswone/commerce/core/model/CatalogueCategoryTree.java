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
public class CatalogueCategoryTree {
  private String id;
  private String key;
  private String parent_id;
  private CatalogueAttributes attributes;
  private List<CatalogueCategoryTree> sub_menu;
}
