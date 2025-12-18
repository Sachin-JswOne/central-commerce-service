package com.jswone.commerce.core.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Slf4j
@Component
public class UomDataSourceInitializer {

    private final DataSource uomDataSource;

    public UomDataSourceInitializer(@Qualifier("uomDataSource") DataSource uomDataSource) {
        this.uomDataSource = uomDataSource;
    }

    @PostConstruct
    public void initialize() {
        log.info("Initializing UOM DataSource...");

        try (Connection conn = uomDataSource.getConnection()) {
            log.info("UOM DataSource initialized successfully. Catalog: {}", conn.getCatalog());
        } catch (Exception ex) {
            log.error("UOM DataSource initialization failed: {}", ex.getMessage());
        }
    }
}
