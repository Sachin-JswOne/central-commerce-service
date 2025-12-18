package com.jswone.commerce.core.service.recentSearch;

import java.util.List;

public interface RecentSearchService {
    List<String> getRecentSearches(int limit);
}
