package com.jswone.commerce.core.model.mou;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class MouDashboardDTO {

    private String mouId;
    private String financialYear;
    private String mouType;
    private MouYearlySummaryDTO mouYearlySummary;
    private List<ProductCategoryDTO> productCategories;
}
