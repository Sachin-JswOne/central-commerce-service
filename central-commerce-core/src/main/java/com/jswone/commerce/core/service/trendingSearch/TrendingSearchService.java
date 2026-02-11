package com.jswone.commerce.core.service.trendingSearch;

import com.jswone.commerce.core.model.elastic.index.UserSearchLogsIndex;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Set;

public interface TrendingSearchService {
    void barTerms(@NotEmpty(message = "bar Terms cannot be empty") Set<String> barTerms);

    void addTrendingSearchTerm(UserSearchLogsIndex userSearchLogsIndex);

    List<String> getTrendingSearchTerms(int limit, String daysOffset);
}
