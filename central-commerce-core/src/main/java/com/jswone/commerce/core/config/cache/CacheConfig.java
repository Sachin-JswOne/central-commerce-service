package com.jswone.commerce.core.config.cache;

import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.config.redis.RedisConfiguration;
import com.jswone.commerce.core.config.redis.RedisErrorWarnHandler;
import com.jswone.commerce.core.config.redis.RedisProperties;
import com.jswone.commerce.core.constants.CacheNames;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SslOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@EnableCaching
@Configuration
@RequiredArgsConstructor
public class CacheConfig {
    private static final Set<Pair<String, Duration>> cache = Set.of(
            Pair.of(CacheNames.BUY_AGAIN_PRODUCTS_CACHE_PREFIX_V2, duration(1440L)),
            Pair.of(CacheNames.BUY_AGAIN_PRODUCTS_CACHE_PREFIX, duration(1440L)),
            Pair.of(CacheNames.CLEAR_RECENT_SEARCHES_CACHE_PREFIX, null),
            Pair.of(CacheNames.BARRED_TRENDING_SEARCHES_CACHE_PREFIX, null),
            Pair.of(CacheNames.DEDUPE_TRENDING_SEARCHES_CACHE_PREFIX, duration(43200L)),
            Pair.of(CacheNames.LOCATION_MASTER_ALL, Duration.ZERO),
            Pair.of(CacheNames.SEO_PRODUCT_TYPES, duration(1440L)),
            Pair.of(CacheNames.SEO_CATEGORY_LOCATIONS, duration(60L)),
            Pair.of(CacheNames.SHORT_LINK_CACHE_PREFIX, null));

    private final CommerceValueConfig commerceValueConfig;

    private final RedisProperties redisProperties;

    private final RedisConfiguration redisConfiguration;

    private static Duration duration(long minutes) {
        return Duration.ofMinutes(minutes);
    }

    private Map<String, RedisCacheConfiguration> getRedisCacheConfigMap(Pair<String, Duration> pair) {
        return Map.of(pair.getLeft(), this.getConfig(pair.getRight()));
    }

    @Bean
    @ConditionalOnProperty(name = "central.commerce.redis.cache_manager.enable", havingValue = "true")
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        log.info("-----ENABLING REDIS AS SPRING CACHE MANAGER-----");
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // adding cacheProfile to every key of cache
        String cacheProfile = commerceValueConfig.getRedisCacheProfile();
        String completeCacheName = cacheProfile.isBlank() ? cacheProfile : cacheProfile.concat(":");
        cache.stream()
                .map(this::getRedisCacheConfigMap)
                .forEach(
                        configMap -> {
                            Map<String, RedisCacheConfiguration> prefixedConfigMap = new HashMap<>();
                            configMap.forEach(
                                    (key, value) -> prefixedConfigMap.put(completeCacheName + key, value));
                            cacheConfigurations.putAll(prefixedConfigMap);
                        });

        return RedisCacheManager.RedisCacheManagerBuilder.fromConnectionFactory(redisConnectionFactory)
                .cacheWriter(RedisCacheWriter.lockingRedisCacheWriter(redisConnectionFactory))
                .disableCreateOnMissingCache()
                .cacheDefaults(this.getDefaultCacheConfig())
                .withInitialCacheConfigurations(cacheConfigurations)
                .disableCreateOnMissingCache()
                .build();
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        redisTemplate.afterPropertiesSet(); // IMPORTANT

        return redisTemplate;
    }

    private RedisCacheConfiguration getConfig(Duration duration) {
        if (Objects.isNull(duration))
            return getDefaultCacheConfig();

        return getDefaultCacheConfig().entryTtl(duration);
    }

    private RedisCacheConfiguration getDefaultCacheConfig() {
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        return RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues() // Don't cache null values
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .computePrefixWith(cacheName -> cacheName.concat(":"));
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisProperties.getHost());
        redisConfig.setPort(redisProperties.getPort());
        redisConfig.setUsername(redisProperties.getUsername());
        redisConfig.setPassword(redisProperties.getPassword());

        LettuceClientConfiguration.LettuceClientConfigurationBuilder lettuceClientConfigurationBuilder = LettuceClientConfiguration
                .builder();

        SslOptions sslOptions = null;
        try {
            String pem;
            if (commerceValueConfig.getRedisCacheProfile().equals("local")) {
                pem = redisConfiguration.getPemContentFromClassPath();
                log.info("Fetched PEM certificate from class path for Redis connection.");
            } else {
                pem = redisConfiguration.getPemContent();
                log.info("Fetched PEM certificate from secret manager for Redis connection.");
            }
            sslOptions = SslOptions.builder().sslContext(CacheClientConfig.createTrustStoreSSLContext(pem)).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ClientOptions clientOptions = ClientOptions
                .builder()
                .sslOptions(sslOptions)
                .protocolVersion(ProtocolVersion.RESP3)
                .build();
        lettuceClientConfigurationBuilder
                .clientOptions(clientOptions)
                .useSsl();

        log.debug(
                "Creating LettuceConnectionFactory with host: {}, port: {}, username: {}",
                redisProperties.getHost(),
                redisProperties.getHost(),
                redisProperties.getUsername());

        LettuceClientConfiguration lettuceClientConfiguration = lettuceClientConfigurationBuilder.build();
        return new LettuceConnectionFactory(redisConfig, lettuceClientConfiguration);
    }

    @Bean
    public CacheErrorHandler errorHandler() {
        return new RedisErrorWarnHandler();
    }
}