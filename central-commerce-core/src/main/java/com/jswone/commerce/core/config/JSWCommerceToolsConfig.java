package com.jswone.commerce.core.config;

import com.commercetools.api.client.ApiInternalLoggerFactory;
import com.commercetools.api.client.ApiRoot;
import com.commercetools.api.client.ByProjectKeyRequestBuilder;
import com.commercetools.api.client.ConcurrentModificationMiddleware;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.api.defaultconfig.ServiceRegion;
import com.commercetools.http.okhttp4.CtOkHttp4Client;
import io.vrap.rmf.base.client.ResponseSerializer;
import io.vrap.rmf.base.client.VrapHttpClient;
import io.vrap.rmf.base.client.error.HttpExceptionFactory;
import io.vrap.rmf.base.client.http.ErrorMiddleware;
import io.vrap.rmf.base.client.http.InternalLoggerMiddleware;
import io.vrap.rmf.base.client.oauth2.ClientCredentials;
import lombok.extern.log4j.Log4j2;
import okhttp3.Protocol;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.Arrays;

/**
 * Configuration class to set the CT Configuration parameters
 */
@Configuration
@PropertySource("classpath:dev.properties")
@Log4j2
public class JSWCommerceToolsConfig {

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

    private static final String JSW_CLIENT = "jsw-one-platform";

    public String getAnonScopes() {
        return anonScopes;
    }

    public void setAnonScopes(String anonScopes) {
        this.anonScopes = anonScopes;
    }

    private ApiRoot apiRoot;

    private ApiRoot getApiRoot() {

        if (ObjectUtils.isEmpty(apiRoot)) {
            VrapHttpClient httpClient =
                    new CtOkHttp4Client(
                            builder ->
                                    builder.connectTimeout(Duration.ofSeconds(ctConnectionTimeout))
                                            .writeTimeout(Duration.ofSeconds(ctWriteTimeout))
                                            .readTimeout(Duration.ofSeconds(ctReadTimeout))
                                            .protocols(
                                                    Arrays.asList(
                                                            Protocol.HTTP_2, Protocol.HTTP_1_1)));

            apiRoot =
                    ApiRootBuilder.of(httpClient)
                            .withErrorMiddleware(
                                    ErrorMiddleware.of(
                                            HttpExceptionFactory.of(ResponseSerializer.of())))
                            .withRetryMiddleware(
                                    ctErrorRetryCount,
                                    Arrays.asList(500, 503, 504)) // HTTP Status to retry
                            .withMiddleware(ConcurrentModificationMiddleware.of(ctErrorRetryCount))
                            .addNotFoundExceptionMiddleware()
                            .withUserAgentSupplier(() -> JSW_CLIENT)
                            .withInternalLoggerMiddleware(
                                    InternalLoggerMiddleware.of(ApiInternalLoggerFactory::get))
                            .defaultClient(
                                    ClientCredentials.of()
                                            .withClientId(clientId)
                                            .withClientSecret(clientSecret)
                                            .withScopes(scopes)
                                            .build(),
                                    ServiceRegion.GCP_AUSTRALIA_SOUTHEAST1)
                            .build();
        }
        return apiRoot;
    }

    @Bean
    public ByProjectKeyRequestBuilder requestBuilderCTAdmin() {
        ByProjectKeyRequestBuilder byProjectKeyRequestBuilder = null;
        if (ObjectUtils.isNotEmpty(getApiRoot())) {
            byProjectKeyRequestBuilder = apiRoot.withProjectKey(projectKey);
        }
        return byProjectKeyRequestBuilder;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getAnonClientId() {
        return anonClientId;
    }

    public void setAnonClientId(String anonClientId) {
        this.anonClientId = anonClientId;
    }

    public String getAnonClientSecret() {
        return anonClientSecret;
    }

    public void setAnonClientSecret(String anonClientSecret) {
        this.anonClientSecret = anonClientSecret;
    }

    @PreDestroy
    public void closeClientConnection() {
        try {
            if (ObjectUtils.isNotEmpty(apiRoot)) {
                apiRoot.close();
            }
        } catch (Exception exception) {
            log.error(" error while closing API Root client connection ");
        }
    }
}

