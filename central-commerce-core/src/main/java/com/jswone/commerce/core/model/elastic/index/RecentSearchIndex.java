package com.jswone.commerce.core.model.elastic.index;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jswone.commerce.core.model.elastic.dto.ResultContext;
import com.jswone.commerce.core.model.elastic.dto.SearchQuery;
import lombok.*;

import java.util.Date;

@Builder(toBuilder = true)
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecentSearchIndex {

    private String id;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("user_type")
    private String userType;

    @JsonProperty("sf_customer_id")
    private String sfCustomerId;

    @JsonProperty("search_id")
    private String searchId;

    @Builder.Default
    private Date timestamp = new Date();

    @JsonProperty("event_type")
    private String eventType; // RECENT_SEARCH

    @JsonProperty("search_type")
    private String searchType; // FULL / PARTIAL

    private SearchQuery query;

    @JsonProperty("to_be_shown_in_recent")
    @Builder.Default
    private boolean toBeShownInRecent = true;

    @JsonProperty("result_context")
    private ResultContext resultContext;

}
