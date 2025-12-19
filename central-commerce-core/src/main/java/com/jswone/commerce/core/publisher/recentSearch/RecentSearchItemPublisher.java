package com.jswone.commerce.core.publisher.recentSearch;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.jswone.commerce.core.enums.ElasticPublisherEventTypes;
import com.jswone.commerce.core.enums.UserSearchTrackingEventTypes;
import com.jswone.commerce.core.exceptions.ParsingException;
import com.jswone.commerce.core.model.elastic.Event;
import com.jswone.commerce.core.model.elastic.dto.ProductResult;
import com.jswone.commerce.core.model.elastic.dto.ResultContext;
import com.jswone.commerce.core.model.elastic.dto.SearchQuery;
import com.jswone.commerce.core.model.elastic.index.RecentSearchIndex;
import com.jswone.commerce.core.model.request.Search.SearchRequest;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.jswone.commerce.core.constants.JWTConstants.*;
import static com.jswone.commerce.core.enums.ErrorType.INCORRECT_INPUT;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecentSearchItemPublisher {

    private final ObjectMapper objectMapper;

    @Value("${gcp.elastic.tracker.publisher.topic}")
    private String TRACKER_PUBLISHER_TOPIC;


    public void publish(ProductSearchResponse productSearchResponse, SearchRequest searchRequest) {
        try {
            Event event = buildConsumerEvent(productSearchResponse, searchRequest);
            String messageString = objectMapper.writeValueAsString(event);
            log.info("Elastic TRACKER_PUBLISHER_TOPIC: {}", TRACKER_PUBLISHER_TOPIC);
            Publisher publisher = Publisher.newBuilder(Objects.requireNonNull(TRACKER_PUBLISHER_TOPIC)).build();
            ByteString data = ByteString.copyFromUtf8(messageString);
            // Create PubsubMessage with the serialized data
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();
            ApiFuture<String> messageId = publisher.publish(pubsubMessage);
            ApiFutures.addCallback(
                    messageId,
                    new ApiFutureCallback<>() {
                        @Override
                        public void onSuccess(String messageId) {
                            log.info("Published Data to Topic: {}, messageId: {}", publisher.getTopicName(), messageId);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            log.error("Failed to publish Data to Topic: {}, message: {}", publisher.getTopicName(), t.getMessage());
                        }

                    },
                    MoreExecutors.directExecutor()
            );
            log.info("Published Recent Search message: {}", messageString);
            MDC.clear();
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ParsingException("Error serializing ", INCORRECT_INPUT);
        }
    }

    private Event buildConsumerEvent(ProductSearchResponse productSearchResponse, SearchRequest searchRequest) {

        RecentSearchIndex recentSearchIndex = RecentSearchIndex.builder()
                .searchId(searchRequest.getSearchId())
                .searchType(searchRequest.getSearchType().equalsIgnoreCase("Full Search") ? "FULL" : "PARTIAL")
                .eventType(UserSearchTrackingEventTypes.RECENT_SEARCH.getValue())
                .id(UUID.randomUUID().toString())
                .userId(MDC.get(USER_ID_CLAIM))
                .sfCustomerId(MDC.get(SF_ID_CLAIM))
                .userType(MDC.get(USER_TYPE_CLAIM))
                .query(SearchQuery.builder()
                        .raw(searchRequest.getText())
                        .normalized(searchRequest.getText().trim())
                        .build())
                .resultContext(ResultContext.builder()
                        .resultCount(productSearchResponse.getProducts().size())
                        .products(productSearchResponse
                                .getProducts().stream()
                                .map(product ->
                                        ProductResult.builder()
                                                .productMmid(product.getProductMmid())
                                                .productImageUrl(product.getMetaData().getProductMedia().get(0).getPublicUrl())
                                                .productSlug(product.getAttributes().getOrDefault("slug", "").toString())
                                                .productTitle(product.getAttributes().getOrDefault("product_title", "").toString())
                                                .build())
                                .toList())
                        .build())
                .build();

        Map<String, Object> requestMap = objectMapper.convertValue(recentSearchIndex, Map.class);

        return Event.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(ElasticPublisherEventTypes.PUBLISH_RECENT_SEARCH.getValue())
                .payload(requestMap)
                .build();
    }

}
