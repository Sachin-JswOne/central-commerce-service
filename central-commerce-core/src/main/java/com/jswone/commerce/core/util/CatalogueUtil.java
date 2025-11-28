package com.jswone.commerce.core.util;

import com.jswone.commerce.core.model.centralCatalogue.Product;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
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
}
