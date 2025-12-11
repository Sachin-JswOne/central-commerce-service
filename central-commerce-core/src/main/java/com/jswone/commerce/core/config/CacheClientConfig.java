package com.jswone.commerce.core.config;

import io.netty.handler.ssl.SslContextBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.JedisClientConfig;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.function.Consumer;


@Configuration
@RequiredArgsConstructor
public class CacheClientConfig {

    static int connectionTimeout = 5000;
    static int socketTimeout = 5000;

    private static X509Certificate loadCertificate(String cert) {
        try {
            byte[] certBytes = cert.getBytes();
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    private static SSLSocketFactory createTrustStoreSSLSocketFactory(String redisSSLCA)
            throws Exception {
        X509Certificate caCert = loadCertificate(redisSSLCA);
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("redis_ca", caCert);

        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, new SecureRandom());
        return sslContext.getSocketFactory();
    }

    public static JedisClientConfig createJedisClientConfiguration(
            String userName, String password, String redisSSLCA) {
        try {
            // Configure JedisPoolConfig
            DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder();
            builder.connectionTimeoutMillis(connectionTimeout);
            builder.socketTimeoutMillis(socketTimeout);
            builder.blockingSocketTimeoutMillis(socketTimeout);
            builder.ssl(true);
            builder.sslSocketFactory(createTrustStoreSSLSocketFactory(redisSSLCA));

            return builder.user(userName).password(password).build();
        } catch (Exception e) {
            System.err.println("JedisClientConfig failed : " + e.getMessage());
            return null;
        }
    }

    public static Consumer<SslContextBuilder> createTrustStoreSSLContext(String redisSSLCA)
            throws Exception {
        X509Certificate caCert = loadCertificate(redisSSLCA);
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("redis_ca", caCert);

        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, new SecureRandom());

        return sslContextBuilder -> sslContextBuilder.sslContextProvider(sslContext.getProvider());
    }
}

