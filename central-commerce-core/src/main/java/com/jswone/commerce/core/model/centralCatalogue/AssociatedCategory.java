package com.jswone.commerce.core.model.centralCatalogue;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssociatedCategory {
  private String path;
  private String rank;
  private AssociatedCategoryAttributes attributes;
  private String id;
  private List<Traversal> traversal;
}
