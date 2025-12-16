package com.jswone.commerce.core.model.elastic.index;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jswone.commerce.core.model.elastic.dto.ProductResult;
import lombok.*;

import java.time.Instant;

@Builder(toBuilder = true)
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrendingProductIndex {

    private String id;

    private Instant timestamp;

    @JsonProperty("asset_type")
    private String assetType;

    private ProductResult product;

    private Integer count;

}
