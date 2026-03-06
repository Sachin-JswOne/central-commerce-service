package com.jswone.commerce.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchedCategoryTreeResponse {
    private int statusCode;
    private String status;
    private SearchedCategoryTree data;
    private ErrorResponse error;
}
