package com.jswone.commerce.core.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Slf4j
@Component
public class HistoricalDataSourceInitializer {

    private final DataSource historicalDataSource;

    public HistoricalDataSourceInitializer(@Qualifier("historicalDataSource") DataSource historicalDataSource) {
        this.historicalDataSource = historicalDataSource;
    }

    @PostConstruct
    public void initialize() {
        log.info("Initializing Historical DataSource...");

        try (Connection conn = historicalDataSource.getConnection()) {
            log.info("Historical DataSource initialized successfully. DataSource: {}", conn.getCatalog());
        } catch (Exception ex) {
            log.error("Historical DataSource initialization failed: {}", ex.getMessage());
        }
    }
}
