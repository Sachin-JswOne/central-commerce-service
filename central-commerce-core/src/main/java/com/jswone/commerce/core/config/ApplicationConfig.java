package com.jswone.commerce.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ApplicationConfig {

    private final CommerceValueConfig commerceValueConfig;

    public ApplicationConfig(CommerceValueConfig commerceValueConfig) {
        this.commerceValueConfig = commerceValueConfig;
    }

    @Bean
    @Primary
    public RestTemplate commerceRestTemplate() {

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();

        requestFactory.setConnectTimeout(
                Math.toIntExact(commerceValueConfig.getServiceConnectionTimeOut()));
        requestFactory.setReadTimeout(
                Math.toIntExact(commerceValueConfig.getServiceReadTimeOut()));

        return new RestTemplate(requestFactory);
    }

    /**
     * RetryTemplate with configurable exponential backoff for catalogue page
     * fetches.
     */
    @Bean
    public RetryTemplate catalogueRetryTemplate() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(commerceValueConfig.getCatalogueRetryInitialInterval());
        backOffPolicy.setMultiplier(commerceValueConfig.getCatalogueRetryMultiplier());
        backOffPolicy.setMaxInterval(commerceValueConfig.getCatalogueRetryMaxInterval());

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                commerceValueConfig.getCatalogueRetryMaxAttempts());

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }
}
