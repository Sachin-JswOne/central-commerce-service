package com.jswone.commerce.core.enums;

import java.util.Arrays;

public enum MouType {
    JSW_STEEL;

    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return Arrays.stream(values()).anyMatch(v -> v.name().equals(value));
    }
}
