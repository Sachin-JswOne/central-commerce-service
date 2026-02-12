package com.jswone.commerce.core.model.response.centralCatalogue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DistrictListResponse {
    private int statusCode;
    private String status;
    private Map<String, List<String>> data; // Map of state -> list of districts
}
