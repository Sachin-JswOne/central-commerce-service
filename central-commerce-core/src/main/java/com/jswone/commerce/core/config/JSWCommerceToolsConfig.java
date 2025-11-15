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
import java.time.Duration;
import java.util.Arrays;
import javax.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import okhttp3.Protocol;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/** Configuration class to set the CT Configuration parameters */
@Configuration
@PropertySource("classpath:application.properties")
@Log4j2
public class JSWCommerceToolsConfig {

  private static final String JSW_CLIENT = "jsw-one-platform";

  private final CommerceValueConfig commerceValueConfig;

  public JSWCommerceToolsConfig(CommerceValueConfig commerceValueConfig) {
    this.commerceValueConfig = commerceValueConfig;
  }

  private ApiRoot apiRoot;

  private ApiRoot getApiRoot() {

    if (ObjectUtils.isEmpty(apiRoot)) {
      VrapHttpClient httpClient =
          new CtOkHttp4Client(
              builder ->
                  builder
                      .connectTimeout(
                          Duration.ofSeconds(commerceValueConfig.getCtConnectionTimeout()))
                      .writeTimeout(Duration.ofSeconds(commerceValueConfig.getCtWriteTimeout()))
                      .readTimeout(Duration.ofSeconds(commerceValueConfig.getCtReadTimeout()))
                      .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1)));

      apiRoot =
          ApiRootBuilder.of(httpClient)
              .withErrorMiddleware(
                  ErrorMiddleware.of(HttpExceptionFactory.of(ResponseSerializer.of())))
              .withRetryMiddleware(
                  commerceValueConfig.getCtErrorRetryCount(),
                  Arrays.asList(500, 503, 504)) // HTTP Status to retry
              .withMiddleware(
                  ConcurrentModificationMiddleware.of(commerceValueConfig.getCtErrorRetryCount()))
              .addNotFoundExceptionMiddleware()
              .withUserAgentSupplier(() -> JSW_CLIENT)
              .withInternalLoggerMiddleware(
                  InternalLoggerMiddleware.of(ApiInternalLoggerFactory::get))
              .defaultClient(
                  ClientCredentials.of()
                      .withClientId(commerceValueConfig.getClientId())
                      .withClientSecret(commerceValueConfig.getClientSecret())
                      .withScopes(commerceValueConfig.getScopes())
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
      byProjectKeyRequestBuilder = apiRoot.withProjectKey(commerceValueConfig.getProjectKey());
    }
    return byProjectKeyRequestBuilder;
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
