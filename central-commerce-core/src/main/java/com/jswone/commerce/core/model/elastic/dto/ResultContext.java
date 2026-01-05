package com.jswone.commerce.core.model.elastic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Builder(toBuilder = true)
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultContext {
    @JsonProperty("result_count")
    private int resultCount;

    private List<ProductResult> products;
}
