package com.jswone.commerce.core.config;

import com.jswone.commerce.core.constants.BuyAgainConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {

    private final CommerceValueConfig commonProperties;

    @Bean
    @ConditionalOnProperty(name = "cache.redis.enabled", havingValue = "false")
    public CacheManager inMemoryCacheManager() {
        log.info("Initializing in-memory cache (ConcurrentMapCacheManager)");
        return new ConcurrentMapCacheManager(BuyAgainConstants.CACHE_NAMES);
    }

    @Bean
    @ConditionalOnProperty(name = "cache.redis.enabled", havingValue = "true")
    public CacheManager cacheManager(
            RedisConnectionFactory redisConnectionFactory) {

        log.info("Initializing RedisCacheManager with TTL={} hours and prefix={}",
                commonProperties.getTtlHours(), commonProperties.getPrefix());

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(commonProperties.getTtlHours()))
                .computePrefixWith(cacheName -> commonProperties.getPrefix())
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> initialConfigs =
                redisCacheConfigurationMap(commonProperties.getCacheNameExpiryMap(), config);

        log.info("Loaded per-cache TTLs: {}", commonProperties.getCacheNameExpiryMap());

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(initialConfigs)
                .build();
    }

    private Map<String, RedisCacheConfiguration> redisCacheConfigurationMap(
            Map<String, Long> cacheNameExpiryMap,
            RedisCacheConfiguration config) {

        return cacheNameExpiryMap.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> config.entryTtl(Duration.ofSeconds(entry.getValue()))
        ));
    }
}
