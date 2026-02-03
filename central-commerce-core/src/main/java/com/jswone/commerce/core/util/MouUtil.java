package com.jswone.commerce.core.util;

import com.jswone.commerce.core.enums.MouType;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import java.util.regex.Pattern;

@Slf4j
public class MouUtil {
    private static final Pattern GSTIN_PATTERN =
            Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");

    private static final Pattern FIN_YEAR_PATTERN = Pattern.compile("^\\d{4}-\\d{2}$");

    private MouUtil() {
    }

    public static void validateGstIn(String gstin) {
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

    public static void validateInput(String mouId, String financialYear, String mouType) {
        if (StringUtils.isBlank(mouId)) {
            log.error("Mou Id must not be null or empty");
            throw new CentralCommerceServiceException("MOU ID cannot be blank", HttpStatus.BAD_REQUEST);
        }
        if (!MouType.isValid(mouType)) {
            log.error("Unsupported MoU type, mouType={}", mouType);
            throw new CentralCommerceServiceException(String.format("Unsupported MoU type, mouType=%s", mouType), HttpStatus.BAD_REQUEST);
        }
        validateFinancialYear(financialYear);
    }
}
