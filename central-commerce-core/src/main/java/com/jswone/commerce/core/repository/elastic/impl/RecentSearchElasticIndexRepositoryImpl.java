package com.jswone.commerce.core.repository.elastic.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.mapping.FieldType;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.jswone.commerce.core.constants.ElasticConstants;
import com.jswone.commerce.core.model.elastic.index.RecentSearchIndex;
import com.jswone.commerce.core.repository.elastic.RecentSearchElasticIndexRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecentSearchElasticIndexRepositoryImpl implements RecentSearchElasticIndexRepository {

    private final ElasticsearchClient elasticsearchClient;

    @Override
    public void insertData(RecentSearchIndex recentSearch) {
        IndexRequest<RecentSearchIndex> request = IndexRequest.of(i -> i
                .id(recentSearch.getId())
                .index(ElasticConstants.RECENT_SEARCH_INDEX)
                .document(recentSearch)
        );

        IndexResponse response = null;
        try {
            response = elasticsearchClient.index(request);
        } catch (IOException e) {
            log.error("Failed to index data : {}", recentSearch.getSearchId());
        }
        log.info("Indexed Search logs ID: {}", response.id());
    }
}
