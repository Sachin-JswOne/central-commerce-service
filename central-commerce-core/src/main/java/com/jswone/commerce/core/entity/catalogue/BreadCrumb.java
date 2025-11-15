package com.jswone.commerce.core.entity.catalogue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.cloud.spring.data.datastore.core.mapping.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
public class BreadCrumb {
  private String name;
  private String categoryId;
  private String categoryKey;
  private String description;
  private String slug;
  private boolean hasPartingCharges;
}
