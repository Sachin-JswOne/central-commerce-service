package com.jswone.commerce.core.model.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceResponse {
  private Integer id;
  private String name;
  private String createdAt;
  private String updatedAt;
}
