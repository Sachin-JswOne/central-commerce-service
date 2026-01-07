package com.jswone.commerce.core.model.elastic.index;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jswone.commerce.core.model.elastic.dto.EventDetails;
import com.jswone.commerce.core.model.elastic.dto.ResultContext;
import lombok.*;

import java.time.Instant;

@Builder(toBuilder = true)
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecentyViewedIndex {

    private String id;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("sf_customer_id")
    private String sfCustomerId;

    @JsonProperty("search_id")
    private String searchId;

    private Instant timestamp;

    @JsonProperty("result_context")
    private ResultContext resultContext;

    @JsonProperty("event_details")
    private EventDetails eventDetails;

}
