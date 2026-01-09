package com.jswone.commerce.core.model.response.centralCatalogue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jswone.commerce.core.model.centralCatalogue.ProductTypeData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductTypeBulkDTO {
    @JsonProperty("product_type_detail")
    private Map<String, ProductTypeData> productTypeDetail;

    @JsonProperty("product_overview")
    private List<Map<String,Object>> productOverview;
}
