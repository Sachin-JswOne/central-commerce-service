package com.jswone.commerce.core.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.*;


import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collectionName = "user_auth_token_store")
public class UserTokenEntity {

    // Firestore document ID = Datastore Key Name
    @DocumentId
    private String tokenHash;

    private String customerId;

    private LocalDateTime addedAt;

    private LocalDateTime expiresAt;
}
