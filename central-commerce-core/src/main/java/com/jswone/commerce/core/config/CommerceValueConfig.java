package com.jswone.commerce.core.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Getter
@Configuration
public class CommerceValueConfig {
    /*
     * Commercetools values.
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

    @Value("${central.catalogue.service.enabled}")
    private boolean centralCatalogueServiceEnabled;

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

    @Value("${central.commerce.redis.cache.enable}")
    private boolean redisEnabled;

    @Value("${central.commerce.cloud.cache.certificate}")
    private String cacheCertificateSecret;

    @Value("${notification.service.v1.token}")
    private String notificationServiceToken;

    @Value("${notification.v1.internal.url}")
    private String notificationV1InternalUrl;

    @Value("${notification.v1.endpoint}")
    private String notificationV1Endpoint;

    @Value("${buy.again.cache.warm.up.teams.workflow.url}")
    private String buyAgainCacheWarmUpTeamsWorkflowUrl;

    @Value("${buy.again.cache.chunk.size}")
    private int buyAgainCacheChunkSize;

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

    // Elastic Configs -- Start
    @Value("${elasticsearch.host}")
    private String elasticsearchHost;

    @Value("${elasticsearch.username}")
    private String elasticUsername;

    @Value("${elasticsearch.password}")
    private String elasticPassword;

    // Connection Pool Configuration Properties
    @Value("${elasticsearch.connection.pool.max-total:200}")
    private int maxTotalConnections;

    @Value("${elasticsearch.connection.pool.max-per-route:100}")
    private int maxConnectionsPerRoute;

    @Value("${elasticsearch.connection.pool.default-max-per-route:10}")
    private int defaultMaxConnectionsPerRoute;

    @Value("${elasticsearch.connection.timeout:5000}")
    private int connectionTimeout;

    @Value("${elasticsearch.socket.timeout:60000}")
    private int socketTimeout;

    @Value("${elasticsearch.connection.request.timeout:5000}")
    private int connectionRequestTimeout;

    @Value("${elasticsearch.connection.keep-alive:300000}")
    private long keepAliveTime;

    @Value("${elasticsearch.connection.idle.timeout:60000}")
    private long idleConnectionTimeout;
    // Elastic Configs -- End

    @Value("${central.catalogue.product.listing.endpoint}")
    private String centralCatalogueProductListingEndpoint;

    @Value("${central.catalogue.product.slug.endpoint}")
    private String centralCatalogueProductSlugEndpoint;

    @Value("${account.master.service.api.key}")
    private String accountMasterServiceApiKey;

    // @Value("${account.master.service.client.id}")
    // private String accountMasterServiceClientId;

    @Value("${account.master.service.base.url}")
    private String accountMasterServiceBaseUrl;

    @Value("${account.master.service.mou.endpoint}")
    private String accountMasterServiceMouEndpoint;

    @Value("${central.catalogue.admin.get.state.endpoint}")
    private String centralCatalogueAdminGetStateEndpoint;

    @Value("${central.catalogue.admin.get.district.endpoint}")
    private String centralCatalogueAdminGetDistrictEndpoint;

    @Value("${gcp.bucket.seo}")
    private String seoBucketName;

    @Value("${sitemap.base.url}")
    private String sitemapBaseUrl;

    @Value("${sitemap.xml.url.prefix}")
    private String sitemapXmlUrlPrefix;

    @Value("${sitemap.disabled.parent.category}")
    private Set<String> disabledParentCategorySet;
}
