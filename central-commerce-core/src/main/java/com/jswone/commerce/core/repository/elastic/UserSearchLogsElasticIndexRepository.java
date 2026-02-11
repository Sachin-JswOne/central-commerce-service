package com.jswone.commerce.core.repository.elastic;

import com.jswone.commerce.core.model.elastic.index.UserSearchLogsIndex;

public interface UserSearchLogsElasticIndexRepository {
    void insertData(UserSearchLogsIndex recentSearch);
}
