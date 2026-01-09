package com.jswone.commerce.core.model.response.centralCatalogue;


import com.jswone.commerce.core.model.centralCatalogue.ProductTypeData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductTypeBulkResponse {
    private int statusCode;
    private String status;
    private ProductTypeBulkDTO data;
}
