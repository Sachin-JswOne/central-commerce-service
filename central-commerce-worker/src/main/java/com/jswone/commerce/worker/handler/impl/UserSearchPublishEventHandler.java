package com.jswone.commerce.worker.handler.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jswone.commerce.core.enums.ElasticPublisherEventTypes;
import com.jswone.commerce.core.model.elastic.index.UserSearchLogsIndex;
import com.jswone.commerce.core.repository.elastic.UserSearchLogsElasticIndexRepository;
import com.jswone.commerce.worker.handler.EventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class UserSearchPublishEventHandler implements EventHandler {

    private final UserSearchLogsElasticIndexRepository userSearchLogsElasticIndexRepository;
    private final ObjectMapper objectMapper;

    public UserSearchPublishEventHandler(UserSearchLogsElasticIndexRepository userSearchLogsElasticIndexRepository, ObjectMapper objectMapper) {
        this.userSearchLogsElasticIndexRepository = userSearchLogsElasticIndexRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handleEvent(Map<String, Object> data) throws Exception {
        UserSearchLogsIndex message = objectMapper.convertValue(data, UserSearchLogsIndex.class);
        try {
            userSearchLogsElasticIndexRepository.insertData(message);
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    @Override
    public String getEventType() {
        return ElasticPublisherEventTypes.PUBLISH_USER_SEARCH_LOGS.getValue();
    }
}
