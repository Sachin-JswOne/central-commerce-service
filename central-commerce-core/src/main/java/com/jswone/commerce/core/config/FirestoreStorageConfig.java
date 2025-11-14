package com.jswone.commerce.core.config;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FirestoreStorageConfig {
    private final CommerceValueConfig commerceValueConfig;

    public FirestoreStorageConfig(CommerceValueConfig commerceValueConfig) {
        this.commerceValueConfig = commerceValueConfig;
    }

    @Bean
    public Firestore firestore() {
        FirestoreOptions.Builder builder = FirestoreOptions.newBuilder()
                .setProjectId(commerceValueConfig.getProjectId());

        return builder.build().getService();
    }
}