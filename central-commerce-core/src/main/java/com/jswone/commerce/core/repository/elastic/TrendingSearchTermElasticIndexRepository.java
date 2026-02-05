package com.jswone.commerce.core.repository.elastic;

import com.jswone.commerce.core.model.elastic.index.TrendingSearchTermIndex;


public interface TrendingSearchTermElasticIndexRepository {
    void insertData(TrendingSearchTermIndex trendingSearchTermIndex);
}
