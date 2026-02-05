package com.jswone.commerce.core.service.trendingSearch.impl;

import com.jswone.commerce.core.model.elastic.index.TrendingSearchTermIndex;
import com.jswone.commerce.core.model.elastic.index.UserSearchLogsIndex;
import com.jswone.commerce.core.repository.elastic.RecentSearchElasticIndexRepository;
import com.jswone.commerce.core.repository.elastic.TrendingSearchTermElasticIndexRepository;
import com.jswone.commerce.core.service.JedisBloomService;
import com.jswone.commerce.core.service.trendingSearch.TrendingSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrendingSearchServiceImpl implements TrendingSearchService {

    private final JedisBloomService jedisBloomService;
    private final TrendingSearchTermElasticIndexRepository trendingSearchTermElasticIndexRepository;

    @Override
    public void barTerms(Set<String> barTermsSet) {
        jedisBloomService.barTerm(barTermsSet);
    }

    @Override
    public void addTrendingSearchTerm(UserSearchLogsIndex userSearchLogsIndex) {
        String redisKey = userSearchLogsIndex.getUserId().concat(":").concat(userSearchLogsIndex.getQuery().getNormalized());
        if (jedisBloomService.addToBloom(redisKey)) {
            trendingSearchTermElasticIndexRepository.insertData(TrendingSearchTermIndex.builder()
                    .id(getQueryString(userSearchLogsIndex.getQuery().getNormalized()))
                    .query(userSearchLogsIndex.getQuery().getNormalized())
                    .build());
        }

    }

    private String getQueryString(String normalized) {
        return String.join("_", normalized.split(" "));
    }
}
