package com.jswone.commerce.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "catalogue")
public class CatalogueDynamicConfig {

    /**
     * Comma separated list → auto converted to List<String>
     */
    private List<String> excludedAttributes;

    /**
     * key:value,key2:value2 → auto converted to Map<String, String>
     */
    private Map<String, String> unitMap;
}
