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
public class CatalogueCategoryTreeResponse {
  private int statusCode;
  private String status;
  private List<CatalogueCategoryTree> data;
  private ErrorResponse error;
}
