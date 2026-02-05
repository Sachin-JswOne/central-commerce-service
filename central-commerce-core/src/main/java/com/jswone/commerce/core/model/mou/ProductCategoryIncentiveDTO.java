package com.jswone.commerce.core.model.mou;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductCategoryIncentiveDTO {

    private String mouId;
    private String financialYear;
    private String mouType;
    private String productCategory;
    private JsonNode incentiveDetails;
}
