package com.jswone.commerce.core.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Slf4j
@Component
public class MouDataSourceInitializer {
    private final DataSource mouDataSource;

    public MouDataSourceInitializer(@Qualifier("mouDataSource") DataSource mouDataSource) {
        this.mouDataSource = mouDataSource;
    }

    @PostConstruct
    public void initialize() {
        log.info("Initializing MOU DataSource...");

        try (Connection conn = mouDataSource.getConnection()) {
            log.info("MOU DataSource initialized successfully. DataSource: {}", conn.getCatalog());
        } catch (Exception ex) {
            log.error("MOU DataSource initialization failed: {}", ex.getMessage());
        }
    }
}

