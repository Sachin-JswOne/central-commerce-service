package com.jswone.commerce.core.repository.elastic;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.jswone.commerce.core.model.elastic.index.RecentSearchIndex;
import com.jswone.commerce.core.model.elastic.index.TrendingSearchTermIndex;

import java.io.IOException;


public interface TrendingSearchTermElasticIndexRepository {
    void insertData(TrendingSearchTermIndex trendingSearchTermIndex);
    void deleteData(String normalizedQuery);
    SearchResponse<TrendingSearchTermIndex> getTrendingSearchTerms(int limit, String offSet) throws IOException;
}
