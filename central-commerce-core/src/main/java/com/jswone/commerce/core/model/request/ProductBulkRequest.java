package com.jswone.commerce.core.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductBulkRequest {
    private List<String> product_mmids;
    private String storefront;
    private String locale;
}
