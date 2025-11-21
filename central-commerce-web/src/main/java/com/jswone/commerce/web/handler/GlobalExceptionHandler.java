package com.jswone.commerce.web.handler;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.jswone.commerce.core.exceptions.CentralCatalogueServiceException;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.exceptions.ProductSelectorException;
import com.jswone.commerce.core.exceptions.UserTokenException;
import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ================================================================
    // 1. BUSINESS EXCEPTIONS
    // ================================================================

    @ExceptionHandler(UserTokenException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserTokenException(UserTokenException ex) {
        log.error("UserTokenException:", ex);
        return buildErrorResponse(BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(CentralCommerceServiceException.class)
    public ResponseEntity<ApiResponse<Object>> handleCommerceException(CentralCommerceServiceException ex) {
        log.error("CentralCommerceServiceException:", ex);
        return buildErrorResponse(ex.getHttpStatus(), ex.getMessage());
    }

    @ExceptionHandler(ProductSelectorException.class)
    public ResponseEntity<ApiResponse<Object>> handleSelectorException(ProductSelectorException ex) {
        log.error("ProductSelectorException:", ex);
        return buildErrorResponse(ex.getHttpStatus(), ex.getMessage());
    }

    @ExceptionHandler(CentralCatalogueServiceException.class)
    public ResponseEntity<ApiResponse<Object>> handleCatalogueException(CentralCatalogueServiceException ex) {
        log.error("CentralCatalogueServiceException:", ex);
        return buildErrorResponse(ex.getHttpStatus(), ex.getMessage());
    }

    // ================================================================
    // 2. VALIDATION & MALFORMED JSON
    // ================================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        log.error("Validation failed:", ex);

        String collected = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return buildErrorResponse(BAD_REQUEST, collected);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        log.error("Malformed JSON:", ex);

        String message = "Malformed JSON request";

        Throwable root = ex.getMostSpecificCause();

        if (root instanceof MismatchedInputException mie) {

            String field = mie.getPath().isEmpty()
                    ? "unknown"
                    : mie.getPath().get(0).getFieldName();

            String expected = (mie.getTargetType() != null)
                    ? mie.getTargetType().getSimpleName()
                    : "valid type";

            String actualValue = extractActualValue(ex.getMessage());

            message = String.format(
                    "Invalid value for field '%s': expected %s, but got '%s'",
                    field, expected, actualValue
            );
        }

        return buildErrorResponse(BAD_REQUEST, message);
    }

    private String extractActualValue(String errorMessage) {
        if (errorMessage == null) return "unknown";

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
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.error("Method not allowed:", ex);
        return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed");
    }

    // ================================================================
    // 4. 404 HANDLING
    // ================================================================

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResourceFound(NoResourceFoundException ex) {
        log.error("NoResourceFoundException: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND,
                "The requested endpoint was not found on this server.");
    }

    // ================================================================
    // 5. FALLBACK HANDLERS
    // ================================================================

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntime(RuntimeException ex) {
        log.error("RuntimeException:", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong. Please try again.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneric(Exception ex) {
        log.error("Unhandled exception:", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error");
    }

    // ================================================================
    // Common Error builder (now returns ResponseEntity)
    // ================================================================

    private ResponseEntity<ApiResponse<Object>> buildErrorResponse(HttpStatus status, String message) {

        ApiResponse<Object> body = ApiResponse.builder()
                .success(false)
                .status(status)
                .error(new ErrorResponse(status.value(), message))
                .data(null)
                .build();

        return new ResponseEntity<>(body, status);  // IMPORTANT
    }
}
