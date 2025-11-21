package com.jswone.commerce.core.config;

import com.jswone.commerce.core.constants.CacheNames;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SslOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@EnableCaching
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "central.commerce.redis.cache_manager.enable", havingValue = "true")
public class CacheConfig {
    private static final Set<Pair<String, Duration>> cache =
            Set.of(
                    Pair.of(CacheNames.BUY_AGAIN_PRODUCTS, duration(1440L)));
    private final RedisProperties redisProperties;

    private final RedisConfiguration redisConfiguration;
    @Value("${redis.profile}")
    private String cacheProfile;

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
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        return redisTemplate;
    }

    private RedisCacheConfiguration getConfig(Duration duration) {
        if (Objects.isNull(duration)) return getDefaultCacheConfig();

        return getDefaultCacheConfig().entryTtl(duration);
    }

    private RedisCacheConfiguration getDefaultCacheConfig() {
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        return RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues() // Don't cache null values
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)
                )
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .computePrefixWith(cacheName -> cacheName.concat(":"));
    }

    @Bean
    @ConditionalOnProperty(name = "central.commerce.redis.cache_manager.enable", havingValue = "true")
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisProperties.getHost());
        redisConfig.setPort(redisProperties.getPort());
        redisConfig.setUsername(redisProperties.getUsername());
        redisConfig.setPassword(redisProperties.getPassword());

        LettuceClientConfiguration.LettuceClientConfigurationBuilder lettuceClientConfigurationBuilder =
                LettuceClientConfiguration.builder();

        SslOptions sslOptions = null;
        try {
            sslOptions = SslOptions.builder().sslContext(CacheClientConfig.createTrustStoreSSLContext(redisConfiguration.getPemContent())).build();
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