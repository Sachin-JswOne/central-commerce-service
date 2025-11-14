package com.jswone.commerce.core.config;


import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
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

    @Value("${api.key.commerce.service}")
    private String X_API_KEY_COMMERCE_SERVICE;

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${product.sku.double.attributes}")
    private String productSkuDoubleAttributes;
}
