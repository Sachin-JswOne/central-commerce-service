package com.jswone.commerce.core.model.elastic.index;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jswone.commerce.core.model.elastic.dto.ResultContext;
import com.jswone.commerce.core.model.elastic.dto.SearchQuery;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecentSearchIndex extends UserSearchLogsIndex {

}
