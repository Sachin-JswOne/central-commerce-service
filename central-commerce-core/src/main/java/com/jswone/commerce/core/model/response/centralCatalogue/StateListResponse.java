package com.jswone.commerce.core.model.response.centralCatalogue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StateListResponse {
    private int statusCode;
    private String status;
    private List<String> data;
}
