package com.jswone.commerce.core.service.recentSearch;

import java.util.Date;
import java.util.List;

public interface RecentSearchService {
    List<String> getRecentSearches(int limit);
    void clearRecentSearches(String userId, Date clearTime);
}
