package com.jswone.commerce.core.config;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisConfiguration {

    private final RedisProperties redisProperties;

    private final CommerceValueConfig commerceValueConfig;

    @Bean
    public JedisPooled jedisPooled() {
        ConnectionPoolConfig poolConfig = createConnectionPoolConfig();

        String pem = commerceValueConfig.getRedisCacheProfile().equals("qa") ?
                getPemContentFromClassPath() : getPemContent();

//        String pem = getPemContent();

        HostAndPort address = new HostAndPort(redisProperties.getHost(), redisProperties.getPort());
        JedisClientConfig config = CacheClientConfig.createJedisClientConfiguration(
                redisProperties.getUsername(), redisProperties.getPassword(), pem);

        return tryCreateRedisConnectionWithRetry(address, config, poolConfig);
    }

    private ConnectionPoolConfig createConnectionPoolConfig() {
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMaxTotal(redisProperties.getPoolsize());
        poolConfig.setMaxIdle(redisProperties.getMaxIdle());
        poolConfig.setMinIdle(redisProperties.getMinIdle());
        poolConfig.setBlockWhenExhausted(true);
        poolConfig.setMaxWait(Duration.ofSeconds(1));
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(300));
        return poolConfig;
    }

    String getPemContent() {
        String cacheCertificateSecret = commerceValueConfig.getCacheCertificateSecret();
        if (StringUtils.isBlank(cacheCertificateSecret)) {
            log.error("Redis PEM certificate secret name is not configured.");
            throw new IllegalStateException("Missing PEM secret configuration.");
        }
        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            SecretVersionName secretVersionName = SecretVersionName.of(commerceValueConfig.getProjectId(), cacheCertificateSecret, "latest");
            AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);
            String payload = response.getPayload().getData().toStringUtf8();
            if (!isValidPemCertificate(payload)) {
                String message = "Invalid PEM certificate for Redis connection.";
                log.error(message);
                throw new IllegalStateException(message);
            }
            return payload;
        } catch (Exception e) {
            log.error("Failed to fetch Redis certificate from GCP Secret Manager: {}", e.getMessage(), e);
            throw new RuntimeException("Redis certificate fetch failure", e);
        }
    }

    private boolean isValidPemCertificate(String pem) {
        try {
            if (!pem.contains("-----BEGIN CERTIFICATE-----") || !pem.contains("-----END CERTIFICATE-----")) {
                log.warn("PEM certificate content missing BEGIN/END boundaries.");
                return false;
            }

            // Split PEM into individual certs, pick only the first one
            String[] certs = pem.split("(?=-----BEGIN CERTIFICATE-----)");
            String firstCertPem = certs[0];

            // Clean up the base64-encoded part
            String certContent = firstCertPem
                    .replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(certContent);
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(decoded));

            certificate.checkValidity();
            return true;
        } catch (Exception e) {
            log.error("PEM certificate validation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    private JedisPooled tryCreateRedisConnectionWithRetry(HostAndPort address, JedisClientConfig config, ConnectionPoolConfig poolConfig) {
        final int maxAttempts = 5;
        final int baseDelayMs = 500;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                JedisPooled jedis = new JedisPooled(poolConfig, address, config);
                String pong = jedis.ping();
                if ("PONG".equalsIgnoreCase(pong)) {
                    log.info("Redis connection successful on attempt {}/{}", attempt, maxAttempts);
                    return jedis;
                } else {
                    log.warn("Unexpected Redis response on attempt {}/{}: '{}'", attempt, maxAttempts, pong);
                }
            } catch (Exception ex) {
                log.warn("Redis connection attempt {}/{} failed: {}", attempt, maxAttempts, ex.getMessage());
                if (attempt == maxAttempts) {
                    throw new IllegalStateException("Redis connection failed after retries.", ex);
                }
                try {
                    Thread.sleep((long) baseDelayMs * (1 << (attempt - 1)));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Retry sleep interrupted.");
                    break;
                }
            }
        }

        throw new IllegalStateException("Redis connection could not be established.");
    }

    private String getPemContentFromClassPath() {
        try {
            ClassPathResource resource = new ClassPathResource("redis_ca.pem");
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}