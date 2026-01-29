package com.jswone.commerce.core.model.request;

import com.jswone.commerce.core.model.response.plp.ProductFilterConditions;

import java.util.List;

public interface FilterRequestProvider {
    List<ProductFilterConditions> getFilterConditions();
}
