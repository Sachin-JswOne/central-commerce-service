package com.jswone.commerce.core.model.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountMasterResponse<T> {
  private Boolean success;
  private String message;
  private ErrorResponseDTO error;
  private T data;
}
