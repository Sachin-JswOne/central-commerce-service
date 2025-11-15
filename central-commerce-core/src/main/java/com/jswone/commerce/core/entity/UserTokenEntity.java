package com.jswone.commerce.core.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.datastore.Key;
import com.google.cloud.spring.data.datastore.core.mapping.Entity;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "user_auth_token_store")
public class UserTokenEntity {
  @Id
  @JsonProperty("identifier")
  Key tokenHash;

  private String customerId;

  private LocalDateTime addedAt;

  private LocalDateTime expiresAt;
}
