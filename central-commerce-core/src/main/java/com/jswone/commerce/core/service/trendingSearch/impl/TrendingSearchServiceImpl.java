package com.jswone.commerce.core.service.trendingSearch.impl;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.jswone.commerce.core.model.elastic.index.TrendingSearchTermIndex;
import com.jswone.commerce.core.model.elastic.index.UserSearchLogsIndex;
import com.jswone.commerce.core.repository.elastic.TrendingSearchTermElasticIndexRepository;
import com.jswone.commerce.core.service.TrendingSearchTermBloomService;
import com.jswone.commerce.core.service.trendingSearch.TrendingSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrendingSearchServiceImpl implements TrendingSearchService {

    private final TrendingSearchTermBloomService trendingSearchTermBloomService;
    private final TrendingSearchTermElasticIndexRepository trendingSearchTermElasticIndexRepository;

    @Override
    public void barTerms(Set<String> barTermsSet) {
        trendingSearchTermBloomService.barTerm(barTermsSet);
        barTermsSet.forEach(term -> trendingSearchTermElasticIndexRepository.deleteData(getQueryString(term)));
    }

    @Override
    public void addTrendingSearchTerm(UserSearchLogsIndex userSearchLogsIndex) {
        String redisKey = userSearchLogsIndex.getUserId().concat(":").concat(userSearchLogsIndex.getQuery().getNormalized());
        if (trendingSearchTermBloomService.addToBloom(redisKey) && !trendingSearchTermBloomService.isBarred(userSearchLogsIndex.getQuery().getNormalized())) {
            trendingSearchTermElasticIndexRepository.insertData(TrendingSearchTermIndex.builder()
                    .id(getQueryString(userSearchLogsIndex.getQuery().getNormalized()))
                    .query(userSearchLogsIndex.getQuery().getNormalized())
                    .build());
        }

    }

    @Override
    public List<String> getTrendingSearchTerms(int limit, String daysOffset) {
        try {
            SearchResponse<TrendingSearchTermIndex> response = trendingSearchTermElasticIndexRepository.getTrendingSearchTerms(limit, daysOffset);
            return response.hits()
                    .hits()
                    .stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .map(TrendingSearchTermIndex::getQuery)
                    .toList();
        } catch (IOException e) {
            log.error("Could not get Trending search items : {} ", e.getMessage());
        }
        return null;
    }

    private String getQueryString(String normalized) {
        return String.join("_", normalized.split(" "));
    }
}
