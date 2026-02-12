package com.jswone.commerce.core.config;

import com.jswone.commerce.core.service.LocationMasterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Warms the location cache at application startup.
 * Implements ApplicationRunner to execute after Spring context is fully
 * initialized.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocationCacheWarmer implements ApplicationRunner {

    private final LocationMasterService locationMasterService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("Starting location cache warming...");
            locationMasterService.warmCache();
            log.info("Location cache warming completed successfully");
        } catch (Exception e) {
            log.error("Location cache warming failed: {}", e.getMessage(), e);
            // Application continues even if cache warming fails
        }
    }
}
