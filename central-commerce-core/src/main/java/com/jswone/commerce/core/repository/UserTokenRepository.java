package com.jswone.commerce.core.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.jswone.commerce.core.entity.UserTokenEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTokenRepository
        extends FirestoreReactiveRepository<UserTokenEntity> {
}
