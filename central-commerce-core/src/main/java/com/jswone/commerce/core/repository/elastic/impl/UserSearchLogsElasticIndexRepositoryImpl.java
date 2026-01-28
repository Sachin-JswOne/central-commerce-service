package com.jswone.commerce.core.repository.elastic.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import com.jswone.commerce.core.constants.ElasticConstants;
import com.jswone.commerce.core.model.elastic.index.UserSearchLogsIndex;
import com.jswone.commerce.core.repository.elastic.UserSearchLogsElasticIndexRepository;
import com.jswone.commerce.core.util.elastic.queryBuilder.RecentSearchQueryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;


@Service
@Slf4j
@RequiredArgsConstructor
public class UserSearchLogsElasticIndexRepositoryImpl implements UserSearchLogsElasticIndexRepository {

    private final ElasticsearchClient elasticsearchClient;

    @Override
    public void insertData(UserSearchLogsIndex userSearchLogs) {
        IndexRequest<UserSearchLogsIndex> request = IndexRequest.of(i -> i
                .id(userSearchLogs.getId())
                .index(ElasticConstants.USER_SEARCH_LOGS)
                .document(userSearchLogs)
        );

        IndexResponse response = null;
        try {
            response = elasticsearchClient.index(request);
        } catch (IOException e) {
            log.error("Failed to index data : {}", userSearchLogs.getSearchId());
        }
        assert response != null;
        log.info("Indexed User Search logs ID: {}", response.id());
    }
}
