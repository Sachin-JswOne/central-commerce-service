package com.jswone.commerce.core.model.masters;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.List;

@lombok.Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDetailBulkResponse {
    @JsonProperty("data")
    private List<Data> data;

}
