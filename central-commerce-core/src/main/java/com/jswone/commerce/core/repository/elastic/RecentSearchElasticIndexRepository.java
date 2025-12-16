package com.jswone.commerce.core.repository.elastic;

import com.jswone.commerce.core.model.elastic.index.RecentSearchIndex;


public interface RecentSearchElasticIndexRepository {
    void insertData(RecentSearchIndex recentSearch);
}
