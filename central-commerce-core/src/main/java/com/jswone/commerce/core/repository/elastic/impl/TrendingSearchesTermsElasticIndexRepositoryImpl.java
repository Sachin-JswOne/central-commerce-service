package com.jswone.commerce.core.repository.elastic.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.mapping.FieldType;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch.core.*;
import com.jswone.commerce.core.constants.ElasticConstants;
import com.jswone.commerce.core.model.elastic.index.TrendingSearchTermIndex;
import com.jswone.commerce.core.repository.elastic.TrendingSearchTermElasticIndexRepository;
import com.jswone.commerce.core.service.TrendingSearchTermBloomService;
import com.jswone.commerce.core.util.elastic.queryBuilder.TrendingSearchQueryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class TrendingSearchesTermsElasticIndexRepositoryImpl implements TrendingSearchTermElasticIndexRepository {

    private final ElasticsearchClient elasticsearchClient;
    private final TrendingSearchQueryBuilder queryBuilder;
    private final TrendingSearchTermBloomService trendingSearchTermBloomService;


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
            log.info("Indexed Trending Search Term ID: {}", response.id());

        } catch (Exception e) {
            log.error("Error while indexing Trending Search Term : {} ", userSearchLogs.getQuery());
        }
    }

    @Override
    public SearchResponse<TrendingSearchTermIndex> getTrendingSearchTerms(int limit, String offset) throws IOException {
        List<String> barredTerms = trendingSearchTermBloomService.getBarredTerms().stream().map(String::new).toList();

        SearchRequest request = SearchRequest.of(s -> s
                .index(List.of(ElasticConstants.TRENDING_SEARCHES_TERMS))
                .sort(so -> so
                        .field(f -> f
                                .field("unique_count")
                                .order(SortOrder.Desc)
                                .unmappedType(FieldType.Float)
                        )
                )
                .query(q -> q
                        .bool(b -> b
                                .mustNot(TermsQuery.of(t -> t
                                                .field("query")
                                                .terms(ta -> ta.value(barredTerms.stream()
                                                        .map(FieldValue::of)
                                                        .toList()
                                                )))
                                        ._toQuery()
                                )
                                .must(queryBuilder.getTrendingSearchTermQuery(offset))
                        )
                )
                .size(limit)
        );

        log.info("Executing search with request: {}", request);
        return elasticsearchClient.search(
                request,
                TrendingSearchTermIndex.class
        );
    }

    @Override
    public void deleteData(String normalizedQuery) {
        try {
            elasticsearchClient.delete(DeleteRequest.of(d -> d
                    .index(ElasticConstants.TRENDING_SEARCHES_TERMS)
                    .id(normalizedQuery)
            ));
        } catch (IOException e) {
            log.error("Error while barring Trending Search Term : {}", normalizedQuery);
        }
    }
}
