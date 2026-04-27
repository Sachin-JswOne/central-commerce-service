package com.jswone.commerce.web.handler;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.jsw.notification_common_model.email.NotificationConfig;
import com.jsw.notification_common_model.email.NotificationData;
import com.jsw.notification_common_model.email.NotificationModel;
import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.exceptions.*;
import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.model.ErrorResponse;
import com.jswone.commerce.core.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.jswone.commerce.core.constants.NotificationConstants.SEARCH_API_FAILURE_MESSAGE;
import static com.jswone.commerce.core.constants.NotificationConstants.TEAMS;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final NotificationService notificationService;
    private final CommerceValueConfig commerceValueConfig;

    public GlobalExceptionHandler(NotificationService notificationService, CommerceValueConfig commerceValueConfig) {
        this.notificationService = notificationService;
        this.commerceValueConfig = commerceValueConfig;
    }

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
    public ResponseEntity<ApiResponse<Object>> handleCatalogueException(
            CentralCatalogueServiceException ex,
            HttpServletRequest request) {
        log.error("CentralCatalogueServiceException:", ex);

        // Check if this is a search API failure
        if (request.getRequestURI().contains("/catalogue/search")) {
            sendSearchFailureNotification(ex, request);
        }

        return buildErrorResponse(ex.getHttpStatus(), ex.getMessage());
    }

    @ExceptionHandler(AccountMasterException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccountMasterException(AccountMasterException ex) {
        log.error("CentralCommerceServiceException:", ex);
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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        log.error("MissingServletRequestParameterException:", ex);
        return buildErrorResponse(BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
        log.error("Validation error:", ex);

        // Extract custom messages from errors
        String errorMessage = ex.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Validation failed");

        return buildErrorResponse(BAD_REQUEST, errorMessage);
    }

    // --- IllegalArgumentException ---
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("IllegalArgumentException: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, " 🚨🚨 FAAAAAAAAAAAH , No illegal activities allowed  🚨🚨");
    }

    // --- AccessDeniedException ---
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(AccessDeniedException ex) {
        log.error("AccessDeniedException: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, " Ever heard of consent? \uD83D\uDC85 It’s giving “ask first, act later” energy — always. \uD83D\uDE09");
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

    // ================================================================
    // Teams Notification for Search API Failures
    // ================================================================

    private void sendSearchFailureNotification(CentralCatalogueServiceException ex, HttpServletRequest request) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String failureReason = ex.getMessage();
            String apiPayload = extractRequestPayload(request);

            String message = String.format(
                    SEARCH_API_FAILURE_MESSAGE,
                    timestamp,
                    failureReason,
                    apiPayload);

            NotificationConfig config = NotificationConfig.builder()
                    .workflowUrl(commerceValueConfig.getSearchApiFailureTeamsWorkflowUrl())
                    .build();

            NotificationModel<Object> notificationModel = NotificationModel.builder()
                    .notificationConfig(config)
                    .notificationData(NotificationData.builder().message(message).build())
                    .channels(List.of(TEAMS))
                    .build();

            notificationService.sendNotificationRequest(notificationModel);

            log.info("Search API failure notification sent to Teams");
        } catch (Exception e) {
            log.error("Failed to send search API failure notification to Teams", e);
        }
    }

    private String extractRequestPayload(HttpServletRequest request) {
        try {
            // Try to get the request body from request attributes if available
            Object requestBody = request.getAttribute("searchRequest");
            if (requestBody != null) {
                return requestBody.toString();
            }

            // Fallback to basic request info
            return String.format("URI: %s, Method: %s, Query: %s",
                    request.getRequestURI(),
                    request.getMethod(),
                    request.getQueryString() != null ? request.getQueryString() : "N/A");
        } catch (Exception e) {
            log.error("Failed to extract request payload", e);
            return "Unable to extract payload";
        }
    }
}
