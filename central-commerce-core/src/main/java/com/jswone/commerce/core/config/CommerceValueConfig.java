package com.jswone.commerce.core.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

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

    @Value("${central.catalogue.admin.bulk.typeid.endpoint}")
    private String centralCatalogueAdminBulkTypeIdEndpoint;

    @Value("${central.catalogue.admin.api.key}")
    private String centralCatalogueAdminApiKey;

    @Value("${central.catalogue.admin.client.id}")
    private String centralCatalogueAdminClientId;

    @Value("${catalogue.category.base.url}")
    private String catalogueCategoryBaseUrl;

    @Value("${catalogue.category.api.key}")
    private String catalogueCategoryApiKey;

    @Value("${catalogue.category.client.id}")
    private String catalogueCategoryClientId;

    @Value("${redis.profile}")
    private String redisCacheProfile;

    @Value("${central.commerce.redis.cache_manager.enable}")
    private boolean redisCacheManagerEnabled;

    @Value("${central.commerce.cloud.cache.certificate}")
    private String cacheCertificateSecret;

    @Value("${pdp.journey.enabled}")
    private boolean pdpJourneyEnabled;

    @Value("${buy.again.warmup.max-entries}")
    private long buyAgainWarmupMaxEntries;

    @Value("${master.data.service.base.url}")
    private String masterDataServiceBaseUrl;

    @Value("${master.data.bulk.product.mmid.endpoint}")
    private String bulkMasterDataProductMMIDEndpoint;

    @Value("${master.data.bulk.product.mmid.api.key}")
    private String bulkMasterDataProductMMIDApiKey;

    @Value("${master.data.bulk.product.mmid.client.id}")
    private String bulkMasterDataProductMMIDClientId;

    @Value("${pricing.service.base.url}")
    private String pricingServiceBaseUrl;

    @Value("${pricing.service.fetch.price.endpoint}")
    private String priceFetchEndpoint;

    @Value("${pricing.service.api.key}")
    private String pricingXApiKey;
}
