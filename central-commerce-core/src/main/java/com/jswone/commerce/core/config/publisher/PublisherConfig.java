package com.jswone.commerce.core.config.publisher;

import com.google.cloud.pubsub.v1.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class PublisherConfig {

    @Bean
    public Publisher recentSearchPublisher(
            @Value("${gcp.elastic.tracker.publisher.topic}") String topic) throws IOException {
        return Publisher.newBuilder(topic).build();
    }
}
