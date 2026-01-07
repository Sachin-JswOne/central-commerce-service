package com.jswone.commerce.core.repository.elastic;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.jswone.commerce.core.model.elastic.index.RecentSearchIndex;

import java.io.IOException;



public interface RecentSearchElasticIndexRepository {
    void insertData(RecentSearchIndex recentSearch);
    SearchResponse<RecentSearchIndex> getRecentSearches(String userId) throws IOException;
}
