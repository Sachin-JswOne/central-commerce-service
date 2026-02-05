package com.jswone.commerce.core.repository.elastic.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.json.JsonData;
import com.jswone.commerce.core.constants.ElasticConstants;
import com.jswone.commerce.core.model.elastic.index.TrendingSearchTermIndex;
import com.jswone.commerce.core.repository.elastic.TrendingSearchTermElasticIndexRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.Map;


@Service
@Slf4j
@RequiredArgsConstructor
public class TrendingSearchesTermsElasticIndexRepositoryImpl implements TrendingSearchTermElasticIndexRepository {

    private final ElasticsearchClient elasticsearchClient;


    @Override
    public void insertData(TrendingSearchTermIndex userSearchLogs) {


        String docId = userSearchLogs.getId();
        String indexName = ElasticConstants.TRENDING_SEARCHES_TERMS;

        GetRequest getRequest = GetRequest.of(g -> g
                .id(docId)
                .index(indexName)
        );

        try {
            GetResponse<TrendingSearchTermIndex> getResponse = elasticsearchClient.get(getRequest, TrendingSearchTermIndex.class);
            TrendingSearchTermIndex existing = null;
            if (getResponse.found()) {
                // Exists: fetch, increment, update (no script)
                existing = getResponse.source();
                assert existing != null;
                existing = existing.toBuilder()
                        .uniqueCount(existing.getUniqueCount() + 1)
                        .timestamp(new Date())
                        .build();
            }


            TrendingSearchTermIndex finalExisting = existing != null ? existing : userSearchLogs;
            IndexRequest<TrendingSearchTermIndex> request = IndexRequest.of(i -> i
                    .id(finalExisting.getId())
                    .index(ElasticConstants.TRENDING_SEARCHES_TERMS)
                    .document(finalExisting)
            );

            IndexResponse response = null;
            try {
                response = elasticsearchClient.index(request);
            } catch (IOException e) {
                log.error("Failed to index data : {}", userSearchLogs.getId());
            }
            assert response != null;
            log.info("Indexed User Search logs ID: {}", response.id());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
