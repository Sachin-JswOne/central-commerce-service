package com.jswone.commerce.core.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    // Historical DB DataSource
    @Bean
    @Primary
    @ConfigurationProperties("historical.datasource")
    public DataSourceProperties historicalDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "historicalDataSource")
    @Primary
    @ConfigurationProperties("historical.datasource.hikari")
    public HikariDataSource historicalDataSource() {
        return historicalDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "historicalJdbcTemplate")
    public JdbcTemplate historicalJdbcTemplate(
            @Qualifier("historicalDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // UOM DB DataSource
    @Bean
    @ConfigurationProperties("uom.service.datasource")
    public DataSourceProperties uomDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "uomDataSource")
    @ConfigurationProperties("uom.service.datasource.hikari")
    public HikariDataSource uomDataSource() {
        return uomDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "uomJdbcTemplate")
    public JdbcTemplate uomJdbcTemplate(
            @Qualifier("uomDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // MoU DB DataSource
    @Bean
    @ConfigurationProperties("mou.datasource")
    public DataSourceProperties mouDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "mouDataSource")
    @ConfigurationProperties("mou.datasource.hikari")
    public HikariDataSource mouDataSource() {
        return mouDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "mouJdbcTemplate")
    public JdbcTemplate mouJdbcTemplate(
            @Qualifier("mouDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}