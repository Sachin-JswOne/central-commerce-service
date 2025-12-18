package com.jswone.commerce.core.model.elastic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Builder(toBuilder = true)
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchQuery {

    private String raw;

    private String normalized;

    @JsonProperty("valid_query_for_trending_search")
    @Builder.Default private boolean validQueryForTrendingSearch = true;
}
