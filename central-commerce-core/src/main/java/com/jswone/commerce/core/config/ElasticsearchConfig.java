package com.jswone.commerce.core.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class ElasticsearchConfig {

    private final CommerceValueConfig commerceValueConfig;

    @Bean
    public ElasticsearchClient elasticsearchClient() {

        log.info("Initializing Elasticsearch client with connection pooling");
        log.info("Connection pool settings - Max Total: {}, Max Per Route: {}, Default Max Per Route: {}",
                commerceValueConfig.getMaxTotalConnections(), commerceValueConfig.getMaxConnectionsPerRoute(), commerceValueConfig.getDefaultMaxConnectionsPerRoute());

        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(commerceValueConfig.getElasticUsername(), commerceValueConfig.getElasticPassword()));

        RestClientBuilder builder = RestClient.builder(HttpHost.create(commerceValueConfig.getElasticsearchHost()))
                .setHttpClientConfigCallback(this::configureHttpClient)
                .setRequestConfigCallback(this::configureRequestConfig);

        RestClient restClient = builder.build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());

        return new ElasticsearchClient(transport);
    }

    /**
     * Configure HTTP client with optimized connection pooling settings
     */
    private HttpAsyncClientBuilder configureHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(commerceValueConfig.getElasticUsername(), commerceValueConfig.getElasticPassword()));

        return httpClientBuilder
                .setDefaultCredentialsProvider(credentialsProvider)
                // Connection Pool Settings
                .setMaxConnTotal(commerceValueConfig.getMaxTotalConnections())
                .setMaxConnPerRoute(commerceValueConfig.getMaxConnectionsPerRoute())
                // Keep-Alive Strategy
                .setKeepAliveStrategy((response, context) -> Duration.ofMillis(commerceValueConfig.getKeepAliveTime()).toMillis())
                // Enable connection reuse
                .setConnectionReuseStrategy((response, context) -> true);

    }

    /**
     * Configure request timeouts
     */
    private RequestConfig.Builder configureRequestConfig(RequestConfig.Builder requestConfigBuilder) {
        return requestConfigBuilder
                .setConnectTimeout(commerceValueConfig.getConnectionTimeout())
                .setSocketTimeout(commerceValueConfig.getSocketTimeout())
                .setConnectionRequestTimeout(commerceValueConfig.getConnectionRequestTimeout());
    }
}