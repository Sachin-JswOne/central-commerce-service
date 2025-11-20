package com.jswone.commerce.core.model.centralCatalogue;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Traversal {
  private String depth;
  private Map<String, Object> attributes;
  private String id;
}
