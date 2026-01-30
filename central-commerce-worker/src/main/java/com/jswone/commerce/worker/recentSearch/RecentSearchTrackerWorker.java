package com.jswone.commerce.worker.recentSearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import com.jswone.commerce.core.enums.ElasticPublisherEventTypes;
import com.jswone.commerce.core.model.elastic.Event;
import com.jswone.commerce.worker.processor.EventProcessor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class RecentSearchTrackerWorker {

    private final ObjectMapper objectMapper;
    private final EventProcessor eventProcessor;
    private final List<String> allowedEvents;

    public RecentSearchTrackerWorker(ObjectMapper objectMapper, EventProcessor eventProcessor, List<String> allowedEvents) {
        this.objectMapper = objectMapper;
        this.eventProcessor = eventProcessor;
        this.allowedEvents = List.of(
                ElasticPublisherEventTypes.PUBLISH_RECENT_SEARCH.getValue()
        );
    }

    @SneakyThrows
    @ServiceActivator(inputChannel = "userSearchTrackerElasticPublisherSubChannel")
    public void worker(String data,
                       @Header(GcpPubSubHeaders.ORIGINAL_MESSAGE) BasicAcknowledgeablePubsubMessage message) {
        try {
            log.debug(" elasticPublisher consumer message ::: {}", message.getProjectSubscriptionName());
            log.debug(" elasticPublisher consumer message data:::{} ", data);
            Event eventData = objectMapper.readValue(data, Event.class);
            if (allowedEvents.contains(eventData.getEventType())) {
                eventProcessor.processEvent(eventData.getEventType(), eventData.getPayload());
                message.ack();
            }
        } catch (Exception e) {
            log.error("Elastic insert failed message:::{}, data:::{} error:{}", message.getProjectSubscriptionName(), data, e.getMessage(), e);
            message.nack();
        }

    }
}
