package com.jswone.commerce.core.model.mou;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ProductCategoryDTO {

    private String mouId;
    private String financialYear;
    private String uom;
    private String mouType;
    private String productCategory;
    private BigDecimal volumeIncentiveRate;
    private BigDecimal consistencyIncentiveRate;
    private BigDecimal msmeIncentiveRate;
    private BigDecimal achievementPercentage;
    private BigDecimal achievedQuantity;
    private BigDecimal remainingQuantity;
    private BigDecimal targetQuantity;
    private BigDecimal savingsAchieved;
    private BigDecimal potentialSavings;
}
