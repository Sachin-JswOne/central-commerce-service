package com.jswone.commerce.core.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CatalogueUtil {

  public static String extractErrorMessage(String responseBody) {
    if (responseBody == null || !responseBody.contains("\"message\"")) {
      return "Unexpected error occurred";
    }
    try {
      int startIndex = responseBody.indexOf("\"message\"") + 10; // after "message":
      int endIndex = responseBody.indexOf("\"", startIndex + 1);
      if (endIndex > startIndex) {
        return responseBody.substring(startIndex + 1, endIndex);
      }
    } catch (Exception ex) {
      log.error("Failed to extract error message: {}", ex.getMessage(), ex);
    }
    return "Unexpected error occurred";
  }
}
