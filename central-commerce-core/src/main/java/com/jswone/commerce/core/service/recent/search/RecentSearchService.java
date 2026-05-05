package com.jswone.commerce.core.service.recent.search;

import java.util.Date;
import java.util.List;

public interface RecentSearchService {
    List<String> getRecentSearches(int limit);
    void clearRecentSearches(String userId, Date clearTime);
}
