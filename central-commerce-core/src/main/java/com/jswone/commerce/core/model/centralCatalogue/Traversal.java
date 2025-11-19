package com.jswone.commerce.core.model.centralCatalogue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Traversal {
  private String depth;
  private AssociatedCategoryAttributes associatedCategoryAttributes;
  private String id;
}
