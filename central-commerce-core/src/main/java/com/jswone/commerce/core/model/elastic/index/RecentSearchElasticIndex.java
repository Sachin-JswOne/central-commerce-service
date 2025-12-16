package com.jswone.commerce.core.model.elastic.index;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jswone.commerce.core.model.elastic.dto.ResultContext;
import com.jswone.commerce.core.model.elastic.dto.SearchQuery;
import lombok.*;

import java.time.Instant;

@Builder(toBuilder = true)
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecentSearchElasticIndex {

    private String id;

    @JsonProperty("guest_user_id")
    private String guestUserId;

    @JsonProperty("sf_customer_id")
    private String sfCustomerId;

    @JsonProperty("search_id")
    private String searchId;

    private Instant timestamp;

    @JsonProperty("event_type")
    private String eventType; // RECENT_SEARCH

    @JsonProperty("search_type")
    private String searchType; // FULL / PARTIAL

    private SearchQuery query;

    @JsonProperty("to_be_shown_in_recent")
    private boolean toBeShownInRecent;

    @JsonProperty("result_context")
    private ResultContext resultContext;

    // getters & setters

}
