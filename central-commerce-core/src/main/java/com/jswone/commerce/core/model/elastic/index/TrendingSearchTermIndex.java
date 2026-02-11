package com.jswone.commerce.core.model.elastic.index;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jswone.commerce.core.model.elastic.dto.ProductResult;
import lombok.*;

import java.time.Instant;
import java.util.Date;

@Builder(toBuilder = true)
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrendingSearchTermIndex {

    private String id;

    @Builder.Default
    private Date timestamp = new Date();

    @JsonProperty("unique_count")
    @Builder.Default
    private int uniqueCount = 1;

    @JsonProperty("is_eligible")
    @Builder.Default
    private boolean isEligible = true;

    @JsonProperty("is_barred")
    @Builder.Default
    private boolean isBarred = false;

    @JsonProperty("query")
    private String query;
}
