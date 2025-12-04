package com.jswone.commerce.core.util;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.text.DecimalFormat;

import static com.jswone.commerce.core.constants.GenericConstants.ALPHABETS;

@Component
@Log4j2
public class PricingUtil {

    public static String generateRandomText(int length) {

        SecureRandom random = new SecureRandom();
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < length; i++) {
            result.append(ALPHABETS[random.nextInt(57)]);
        }
        return String.valueOf(result);
    }

    public static double roundOffDouble(double price, int scale, RoundingMode roundingMode) {
        return BigDecimal.valueOf(price).setScale(scale, roundingMode).doubleValue();
    }

    public static String formatIndianCommaSeparated(long rupee) {
        String raw = String.valueOf(Math.abs(rupee));
        int numDigits = raw.length();
        StringBuilder sb = new StringBuilder(raw);
        sb = sb.reverse();
        int commas = 0;
        for (int i = 0; i < numDigits; i++) {
            if (i % 2 == 1 && i != 1) {
                sb.insert(i + commas, ",");
                commas++;
            }
        }
        String sign = (rupee < 0) ? "-" : "";
        return sign + sb.reverse();
    }

    public static String formatIndianDoubleRupee(double rupee) {
        DecimalFormat df = new DecimalFormat("#.00");
        String formattedRupee = df.format(rupee);

        StringBuilder sb = new StringBuilder(formattedRupee);
        // Remove decimal part for formatting the integer part
        int decimalIndex = sb.indexOf(".");
        String integerPart = sb.substring(0, decimalIndex);
        String decimalPart = sb.substring(decimalIndex);

        sb = new StringBuilder(integerPart).reverse();
        int numDigits = integerPart.length();
        int commas = 0;

        for (int i = 0; i < numDigits; i++) {
            // Insert a comma after every two digits from the left, except the first group
            if (i % 2 == 1 && i != 1) {
                sb.insert(i + commas, ",");
                commas++;
            }
        }

        String sign = (rupee < 0) ? "-" : "";
        return sign + sb.reverse().toString() + decimalPart;
    }
}
