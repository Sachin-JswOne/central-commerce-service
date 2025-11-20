package com.jswone.commerce.core.config;


import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Getter
@Configuration
public class CommerceValueConfig {
    /*
    Commercetools values.
     */
    @Value("${ctp.admin.projectKey}")
    private String projectKey;

    @Value("${ctp.admin.clientId}")
    private String clientId;

    @Value("${ctp.admin.clientSecret}")
    private String clientSecret;

    @Value("${ctp.authUrl}")
    private String authUrl;

    @Value("${ctp.apiUrl}")
    private String apiUrl;

    @Value("${ctp.admin.scopes}")
    private String scopes;

    @Value("${ctp.anon.clientId}")
    private String anonClientId;

    @Value("${ctp.anon.clientSecret}")
    private String anonClientSecret;

    @Value("${ctp.anon.scopes}")
    private String anonScopes;
    @Value("${ct.connection.timeout.seconds}")
    private int ctConnectionTimeout;

    @Value("${ct.write.timeout.seconds}")
    private int ctWriteTimeout;

    @Value("${ct.read.timeout.seconds}")
    private int ctReadTimeout;

    @Value("${ct.error.retry.count}")
    private int ctErrorRetryCount;

    @Value("${api.key.commerce.service.web}")
    private String X_API_KEY_COMMERCE_SERVICE;

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${product.sku.double.attributes}")
    private String productSkuDoubleAttributes;

    @Value("${service.connection.timeout}")
    private long serviceConnectionTimeOut;

    @Value("${service.connection.readTimeout}")
    private long serviceReadTimeOut;

    @Value("${central.catalogue.base.url}")
    private String centralCatalogueBaseUrl;

    @Value("${central.catalogue.generic.search.endpoint}")
    private String centralCatalogueGenericSearchEndpoint;

    @Value("${central.catalogue.api.key}")
    private String centralCatalogueApiKey;

    @Value("${central.catalogue.client.id}")
    private String centralCatalogueClientId;

    @Value("${central.catalogue.bulk.mmid.endpoint}")
    private String centralCatalogueBulkMmidEndpoint;

    @Value("${spring.redis.ttl-hours}")
    private long ttlHours;

    @Value("${spring.redis.prefix}")
    private String prefix;

    @ConfigurationProperties(prefix = "cache.expiry")
    @Bean
    public Map<String, Long> getCacheNameExpiryMap() {
        return new HashMap<>();
    }

}
