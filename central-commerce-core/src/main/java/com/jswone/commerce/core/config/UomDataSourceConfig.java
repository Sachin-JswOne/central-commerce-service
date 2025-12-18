package com.jswone.commerce.core.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UomDataSourceConfig {

    @Bean(name = "uomDataSourceProperties")
    @ConfigurationProperties("uom.service.datasource")
    public DataSourceProperties uomDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "uomDataSource")
    @ConfigurationProperties("uom.service.datasource.hikari")
    public HikariDataSource uomDataSource(
            @Qualifier("uomDataSourceProperties") DataSourceProperties props) {

        return props.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }
}

