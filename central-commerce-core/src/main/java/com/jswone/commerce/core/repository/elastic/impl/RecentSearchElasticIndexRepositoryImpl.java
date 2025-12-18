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
import com.jswone.commerce.core.util.elastic.queryBuilder.RecentSearchQueryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

import static com.jswone.commerce.core.constants.JWTConstants.USER_ID_CLAIM;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecentSearchElasticIndexRepositoryImpl implements RecentSearchElasticIndexRepository {

    private final ElasticsearchClient elasticsearchClient;
    private final RecentSearchQueryBuilder recentSearchQueryBuilder;

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

    @Override
    public SearchResponse<RecentSearchIndex> getRecentSearches(String userId) throws IOException {
        SearchRequest request = SearchRequest.of(s -> s
                .index(List.of(ElasticConstants.RECENT_SEARCH_INDEX))
                .sort(so -> so
                        .field(f -> f
                                .field("timestamp")
                                .order(SortOrder.Desc)
                                .unmappedType(FieldType.Float)
                        )
                )
                .query(recentSearchQueryBuilder.getRecentSearchQuery(userId))
                .size(1000)
        );

        log.info("Executing search with request: {}", request);
        return elasticsearchClient.search(
                request,
                RecentSearchIndex.class
        );
    }
}
