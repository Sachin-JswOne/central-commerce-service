package com.jswone.commerce.core.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Slf4j
@Component
public class CentralCommerceDataSourceInitializer {

    private final DataSource centralCommerceDataSource;

    public CentralCommerceDataSourceInitializer(@Qualifier("centralCommerceDataSource") DataSource centralCommerceDataSource) {
        this.centralCommerceDataSource = centralCommerceDataSource;
    }

    @PostConstruct
    public void initialize() {
        log.info("Initializing Central Commerce DataSource...");

        try (Connection conn = centralCommerceDataSource.getConnection()) {
            log.info("Central Commerce DataSource initialized successfully. DataSource: {}", conn.getCatalog());
        } catch (Exception ex) {
            log.error("Central Commerce DataSource initialization failed: {}", ex.getMessage());
        }
    }
}
