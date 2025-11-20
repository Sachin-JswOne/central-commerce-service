package com.jswone.commerce.web.handler;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.jswone.commerce.core.exceptions.CentralCatalogueServiceException;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.exceptions.ProductSelectorException;
import com.jswone.commerce.core.exceptions.UserTokenException;
import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  // ================================================================
  // 1. BUSINESS EXCEPTIONS
  // ================================================================

  @ExceptionHandler(UserTokenException.class)
  public ApiResponse<Object> handleUserTokenException(UserTokenException ex) {
    log.error("UserTokenException:", ex);
    return buildError(BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(CentralCommerceServiceException.class)
  public ApiResponse<Object> handleCommerceException(CentralCommerceServiceException ex) {
    log.error("CentralCommerceServiceException:", ex);
    return buildError(ex.getHttpStatus(), ex.getMessage());
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
  public ApiResponse<Object> handleSelectorException(ProductSelectorException ex) {
    log.error("ProductSelectorException:", ex);
    return buildError(ex.getHttpStatus(), ex.getMessage());
  }

  @ExceptionHandler(CentralCatalogueServiceException.class)
  public ApiResponse<Object> handleCatalogueException(CentralCatalogueServiceException ex) {
    log.error("CentralCatalogueServiceException:", ex);
    return buildError(ex.getHttpStatus(), ex.getMessage());
  }

  // ================================================================
  // 2. VALIDATION & MALFORMED JSON
  // ================================================================

  /** Catch bean validation errors: @Valid etc */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ApiResponse<Object> handleValidation(MethodArgumentNotValidException ex) {
    log.error("Validation failed:", ex);

    String collected =
        ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .collect(Collectors.joining(", "));

    return buildError(BAD_REQUEST, collected);
  }

  /** Unified JSON parsing / wrong type handling */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ApiResponse<Object> handleInvalidJson(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    log.error("Malformed JSON:", ex);

    String message = "Malformed JSON request";

    Throwable root = ex.getMostSpecificCause();

    if (root instanceof MismatchedInputException mie) {

      // Field name
      String field = mie.getPath().isEmpty() ? "unknown" : mie.getPath().get(0).getFieldName();

      // Expected type
      String expected =
          (mie.getTargetType() != null) ? mie.getTargetType().getSimpleName() : "valid type";

      // Actual received raw value
      String actualValue = extractActualValue(ex.getMessage());

      message =
          String.format(
              "Invalid value for field '%s': expected %s, but got '%s'",
              field, expected, actualValue);
    }

    return buildError(BAD_REQUEST, message);
  }

  /** Extract actual wrong value shown inside JSON parse exception */
  private String extractActualValue(String errorMessage) {
    if (errorMessage == null) return "unknown";

    // Example error message substring:
    // "Cannot deserialize value of type `java.lang.Integer` from String \"abc\""
    int fromIdx = errorMessage.indexOf("from");
    int quoteIdx = errorMessage.indexOf("\"", fromIdx);

    if (quoteIdx == -1) return "unknown";

    int endQuote = errorMessage.indexOf("\"", quoteIdx + 1);
    if (endQuote == -1) return "unknown";

    return errorMessage.substring(quoteIdx + 1, endQuote);
  }

  // ================================================================
  // 3. METHOD NOT SUPPORTED
  // ================================================================

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ApiResponse<Object> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
    log.error("Method not allowed:", ex);
    return buildError(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed");
  }

  // ================================================================
  // 4. 404 HANDLING
  // ================================================================

  @ExceptionHandler(NoResourceFoundException.class)
  public ApiResponse<Object> handleNoResourceFound(NoResourceFoundException ex) {
    log.error("NoResourceFoundException: {}", ex.getMessage());
    return buildError(HttpStatus.NOT_FOUND, "The requested endpoint was not found on this server.");
  }

  // ================================================================
  // 5. FALLBACK HANDLERS
  // ================================================================

  @ExceptionHandler(RuntimeException.class)
  public ApiResponse<Object> handleRuntime(RuntimeException ex) {
    log.error("RuntimeException:", ex);
    return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.");
  }

  @ExceptionHandler(Exception.class)
  public ApiResponse<Object> handleGeneric(Exception ex) {
    log.error("Unhandled exception:", ex);
    return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
  }

  // ================================================================
  // Common Error builder
  // ================================================================

  private ApiResponse<Object> buildError(HttpStatus status, String message) {
    return ApiResponse.builder()
        .success(false)
        .status(status)
        .error(new ErrorResponse(status.value(), message))
        .data(null)
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
