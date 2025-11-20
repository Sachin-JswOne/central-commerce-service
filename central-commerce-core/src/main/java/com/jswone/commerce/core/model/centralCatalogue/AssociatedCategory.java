package com.jswone.commerce.core.model.centralCatalogue;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssociatedCategory {
  private String path;
  private String rank;
  private Map<String, Object> attributes;
  private String id;
  private List<Traversal> traversal;
}
