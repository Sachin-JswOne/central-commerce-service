package com.jswone.commerce.core.util;

import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ApiResponseUtil {

  private ApiResponseUtil() {}

  public static <T> ApiResponse<T> createSuccessResponse(T data, HttpStatus status) {
    return ApiResponse.<T>builder().status(status).data(data).success(true).build();
  }

  public static <T> ApiResponse<T> createErrorResponse(String message, HttpStatus status) {
    return ApiResponse.<T>builder()
        .status(status)
        .success(false)
        .error(ErrorResponse.builder().message(message).code(status.value()).build())
        .build();
  }
}
