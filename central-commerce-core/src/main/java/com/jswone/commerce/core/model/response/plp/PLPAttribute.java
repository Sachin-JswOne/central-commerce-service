package com.jswone.commerce.core.model.response.plp;

import com.google.cloud.spring.data.datastore.core.mapping.Entity;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Entity
public class PLPAttribute {
  private String displayName;
  private String value;
}
