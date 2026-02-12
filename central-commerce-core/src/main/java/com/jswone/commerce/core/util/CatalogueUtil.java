package com.jswone.commerce.core.util;

import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.centralCatalogue.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;

@Component
@Slf4j
public class CatalogueUtil {

    // HELPERS
    public static String extractImage(Product p) {
        try {
            return p.getMetaData().getProductMedia().get(0).getPublicUrl();
        } catch (Exception e) {
            return "";
        }
    }

    public static String extractAlt(Product p) {
        try {
            return p.getMetaData().getProductMedia().get(0).getMetaData().getAltText();
        } catch (Exception e) {
            return "";
        }
    }

    public static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    public static Double safeDouble(Object o) {
        if (o instanceof Number num) return num.doubleValue();
        try {
            return Double.parseDouble(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public static String formatRange(Double min, Double max, String unit) {
        if (min == null && max == null) return "";
        if (min != null && max != null) return trim(min) + " - " + trim(max) + unitSuffix(unit);
        if (min != null) return trim(min) + unitSuffix(unit);
        return trim(max) + unitSuffix(unit);
    }

    public static String trim(Double d) {
        return (d == d.intValue()) ? String.valueOf(d.intValue()) : d.toString();
    }

    public static String formatName(String raw) {
        raw = raw.replace("_", " ").trim();
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    public static String unitSuffix(String u) { return u.isBlank() ? "" : " " + u; }

    // SMART UNIT DETECTION
    public static String getUnitFor(String base, Map<String, String> UNIT_MAP) {
        String key = base.toLowerCase();

        if (UNIT_MAP.containsKey(key)) return UNIT_MAP.get(key);

        if (key.contains("weight")) return "kg/m";
        if (key.contains("diameter")) return "mm";
        if (key.contains("depth")) return "mm";
        if (key.contains("width")) return "mm";
        if (key.contains("thickness")) return "mm";
        if (key.contains("length")) return "mm";

    return "";
  }

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

    /**
     * Splits a Set into chunks of specified size.
     *
     * @param inputSet  The original set to split
     * @param chunkSize The size of each chunk
     * @param <T>       The type of elements in the set
     * @return List of chunks (each chunk is a Set)
     */
    public static <T> List<Set<T>> chunkSet(Set<T> inputSet, int chunkSize) {
        List<Set<T>> chunks = new ArrayList<>();
        List<T> list = new ArrayList<>(inputSet);

        for (int i = 0; i < list.size(); i += chunkSize) {
            Set<T> chunk = new HashSet<>(list.subList(i, Math.min(i + chunkSize, list.size())));
            chunks.add(chunk);
        }
        return chunks;
    }

    /**
     * Extracts product MMID from variant MMID.
     * Variant MMID format: {part1}-{part2}-{variantId}
     * Product MMID format: {part1}-{part2}
     *
     * Example: "1000-10000-10000079" -> "1000-10000"
     */
    public static String extractProductMmid(String variantMmid) {
        if (variantMmid == null || variantMmid.trim().isEmpty()) {
            throw new CentralCommerceServiceException("Variant MMID cannot be null or empty", HttpStatus.BAD_REQUEST);
        }

        String[] parts = variantMmid.split("-");
        if (parts.length < 2) {
            throw new CentralCommerceServiceException(
                    "Invalid variant MMID format: " + variantMmid + ". Expected format: {categoryId}-{ProductId}-{variantId}",
                    HttpStatus.BAD_REQUEST);
        }

        return parts[0] + "-" + parts[1];
    }

    public static String formatSeoLocationNameToUpperCase(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        return input
                .trim()
                .replace("-", " ")
                .toUpperCase();
    }

    public static String formatSeoLocationToTitleCase(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        String[] words = input.trim().toLowerCase().split("-");

        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return result.toString().trim();
    }
}
