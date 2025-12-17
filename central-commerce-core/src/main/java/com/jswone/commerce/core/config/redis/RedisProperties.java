package com.jswone.commerce.core.config.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "central.commerce.redis")
@Configuration
@Data
public class RedisProperties {
    private String username;
    private String password;
    private String host;
    private int port;
    private int timeout;
    private Integer poolsize;
    private Integer minIdle;
    private Integer maxIdle;
}
