package com.jswone.commerce.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;


public class ApplicationConfig {


    private final CommerceValueConfig commerceValueConfig;

    public ApplicationConfig(CommerceValueConfig commerceValueConfig) {
        this.commerceValueConfig = commerceValueConfig;
    }

    @Bean
    @Primary
    public RestTemplate commerceRestTemplate() {

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory();

        requestFactory.setConnectTimeout(
                Math.toIntExact(commerceValueConfig.getServiceConnectionTimeOut()));
        requestFactory.setReadTimeout(
                Math.toIntExact(commerceValueConfig.getServiceReadTimeOut()));

        return new RestTemplate(requestFactory);
    }
}
