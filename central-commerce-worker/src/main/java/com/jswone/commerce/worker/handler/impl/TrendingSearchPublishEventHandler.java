package com.jswone.commerce.worker.handler.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jswone.commerce.core.enums.ElasticPublisherEventTypes;
import com.jswone.commerce.core.model.elastic.index.RecentSearchIndex;
import com.jswone.commerce.core.model.elastic.index.UserSearchLogsIndex;
import com.jswone.commerce.core.repository.elastic.RecentSearchElasticIndexRepository;
import com.jswone.commerce.core.service.trendingSearch.TrendingSearchService;
import com.jswone.commerce.worker.handler.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class TrendingSearchPublishEventHandler implements EventHandler {

    private final TrendingSearchService trendingSearchService;
    private final ObjectMapper objectMapper;


    @Override
    public void handleEvent(Map<String, Object> data) throws Exception {
        UserSearchLogsIndex message = objectMapper.convertValue(data, UserSearchLogsIndex.class);
        try {
            trendingSearchService.addTrendingSearchTerm(message);
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    @Override
    public String getEventType() {
        return ElasticPublisherEventTypes.PUBLISH_TRENDING_SEARCH_TERM.getValue();
    }
}
