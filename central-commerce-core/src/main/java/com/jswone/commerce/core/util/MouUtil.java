package com.jswone.commerce.core.util;

import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import java.util.regex.Pattern;

public class MouUtil {
    private static final Pattern GSTIN_PATTERN =
            Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");

    private static final Pattern FIN_YEAR_PATTERN = Pattern.compile("^\\d{4}-\\d{2}$");

    private MouUtil() {
    }

    public static void validateGstin(String gstin) {
        if (StringUtils.isBlank(gstin) || !GSTIN_PATTERN.matcher(gstin).matches()) {
            throw new CentralCommerceServiceException("Invalid or missing GSTIN", HttpStatus.BAD_REQUEST);
        }
    }

    public static void validateFinancialYear(String financialYear) {
        if (StringUtils.isBlank(financialYear)
                || !FIN_YEAR_PATTERN.matcher(financialYear).matches()) {
            throw new CentralCommerceServiceException("Financial year must be in YYYY-YY format", HttpStatus.BAD_REQUEST);
        }
    }
}
