package com.jswone.commerce.core.config;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class FirestoreStorageConfig {
    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Bean
    public Firestore firestore() {
        FirestoreOptions.Builder builder = FirestoreOptions.newBuilder()
                .setProjectId(projectId);

        return builder.build().getService();
    }
}
