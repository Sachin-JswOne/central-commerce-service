package com.jswone.commerce.worker.config;


import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.MessageChannel;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class UserSearchTrackerSubConfig {


    @Value("${gcp.elastic.tracker.publisher.subscription}")
    private String TRACKER_PUBLISHER_SUBSCRIPTION_ID;

    @Bean
    public MessageChannel userSearchTrackerElasticPublisherSubChannel() {
        return new PublishSubscribeChannel();
    }

    @Bean
    public PubSubInboundChannelAdapter userSearchTrackerElasticPublisherSubChannelAdapter(
            @Qualifier("userSearchTrackerElasticPublisherSubChannel") MessageChannel inputChannel, PubSubTemplate pubSubTemplate) {
        log.info("Creating PubSubInboundChannelAdapter for subscription: {}", TRACKER_PUBLISHER_SUBSCRIPTION_ID);
        PubSubInboundChannelAdapter adapter =
                new PubSubInboundChannelAdapter(pubSubTemplate, TRACKER_PUBLISHER_SUBSCRIPTION_ID);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        adapter.setPayloadType(String.class);
        return adapter;
    }

}

