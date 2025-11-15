package com.jswone.commerce.web.handler;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.exceptions.ProductSelectorException;
import com.jswone.commerce.core.exceptions.UserTokenException;
import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  // ----------------------
  // Custom Commerce Exceptions
  // ----------------------

  @ExceptionHandler(UserTokenException.class)
  public ApiResponse<Object> handleUserTokenException(
      UserTokenException ex, HttpServletRequest request) {
    log.error("Failed to authenticate user Request ", ex);
    String messages = ex.getMessage();
    return ApiResponse.builder()
        .status(BAD_REQUEST)
        .error(new ErrorResponse(BAD_REQUEST.value(), messages))
        .success(false)
        .build();
  }

  @ExceptionHandler(CentralCommerceServiceException.class)
  public ApiResponse<Object> handleCommerceException(
      CentralCommerceServiceException ex, HttpServletRequest request) {
    String messages = ex.getMessage();
    return ApiResponse.builder()
        .status(BAD_REQUEST)
        .error(new ErrorResponse(BAD_REQUEST.value(), messages))
        .success(false)
        .build();
  }

  @ExceptionHandler(ProductSelectorException.class)
  public ApiResponse<Object> handleProductSelectorException(
      ProductSelectorException ex, HttpServletRequest request) {
    String messages = ex.getMessage();
    return ApiResponse.builder()
        .status(BAD_REQUEST)
        .error(new ErrorResponse(BAD_REQUEST.value(), messages))
        .success(false)
        .build();
  }
}
