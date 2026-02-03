package com.jswone.commerce.core.model.mou;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class MouYearlySummaryDTO {

    private String mouId;
    private String financialYear;
    private String mouType;
    private String uom;
    private String lastUpdatedDate;
    private BigDecimal achievementPercentage;
    private BigDecimal achievedQuantity;
    private BigDecimal remainingQuantity;
    private BigDecimal targetQuantity;
    private BigDecimal savingsAchieved;
    private BigDecimal potentialSavings;
}
