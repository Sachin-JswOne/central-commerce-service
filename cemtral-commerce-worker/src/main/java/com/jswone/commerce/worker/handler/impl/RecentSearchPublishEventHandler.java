package com.jswone.commerce.worker.handler.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jswone.commerce.core.enums.ElasticPublisherEventTypes;
import com.jswone.commerce.core.model.elastic.index.RecentSearchIndex;
import com.jswone.commerce.core.repository.elastic.RecentSearchElasticIndexRepository;
import com.jswone.commerce.worker.handler.EventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class RecentSearchPublishEventHandler implements EventHandler {

    private final RecentSearchElasticIndexRepository recentSearchElasticIndexRepository;
    private final ObjectMapper objectMapper;

    public RecentSearchPublishEventHandler(RecentSearchElasticIndexRepository recentSearchElasticIndexRepository, ObjectMapper objectMapper) {
        this.recentSearchElasticIndexRepository = recentSearchElasticIndexRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handleEvent(Map<String, Object> data) throws Exception {
        RecentSearchIndex message = objectMapper.convertValue(data.get("data"), RecentSearchIndex.class);
        try {
            recentSearchElasticIndexRepository.insertData(message);
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    @Override
    public String getEventType() {
        return ElasticPublisherEventTypes.PUBLISH_RECENT_SEARCH.getValue();
    }
}
