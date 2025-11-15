package com.jswone.commerce.core.repository;

import com.google.cloud.datastore.Key;
import com.google.cloud.spring.data.datastore.repository.DatastoreRepository;
import com.jswone.commerce.core.entity.UserTokenEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTokenRepository extends DatastoreRepository<UserTokenEntity, Key> {}
