package com.jswone.commerce.core.service.recentSearch.impl;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.jswone.commerce.core.model.elastic.index.RecentSearchIndex;
import com.jswone.commerce.core.publisher.recentSearch.RecentSearchItemPublisher;
import com.jswone.commerce.core.repository.elastic.RecentSearchElasticIndexRepository;
import com.jswone.commerce.core.service.recentSearch.RecentSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.jswone.commerce.core.constants.JWTConstants.USER_ID_CLAIM;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecentSearchServiceImpl implements RecentSearchService {

    private final RecentSearchElasticIndexRepository recentSearchElasticIndexRepository;
    private final RecentSearchItemPublisher recentSearchItemPublisher;

    @Override
    public List<String> getRecentSearches(int limit) {
        try {
            SearchResponse<RecentSearchIndex> response = recentSearchElasticIndexRepository.getRecentSearches(MDC.get(USER_ID_CLAIM));
            return response.hits()
                    .hits()
                    .stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .filter(recentSearchIndex -> recentSearchIndex.getSearchType().equalsIgnoreCase("full"))
                    .collect(Collectors.toMap(
                            rs -> rs.getQuery().getNormalized(),
                            Function.identity(),
                            (existing, duplicate) -> existing,
                            LinkedHashMap::new
                    ))
                    .values()
                    .stream()
                    .limit(5)
                    .map(recentSearchIndex -> recentSearchIndex.getQuery().getNormalized())
                    .toList();

        } catch (IOException e) {
            log.error("Exception occurred while fetching recent_searches");
        }

        return null;
    }

    @Override
    public void clearRecentSearches(String userId) {
        try {
            SearchResponse<RecentSearchIndex> response = recentSearchElasticIndexRepository.getRecentSearches(userId);
            List<RecentSearchIndex> recentSearchIndexes = response.hits()
                    .hits()
                    .stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .toList();

            if (recentSearchIndexes.isEmpty()) {
                log.info("No recent recentSearchIndexes found for userId {}", userId);
            }

            log.info("Clearing {} recent recentSearchIndexes for userId {}",
                    recentSearchIndexes.size(), userId);

            for (RecentSearchIndex recentSearchIndex : recentSearchIndexes) {
                if (recentSearchIndex.isToBeShownInRecent()) {
                    log.info("Clearing recentSearchIndex searchId: {} for userId: {}", recentSearchIndex.getSearchId(), userId);
                    RecentSearchIndex updatedRecentSearchIndex = recentSearchIndex.toBuilder().toBeShownInRecent(false).build();
                    recentSearchItemPublisher.publishClearRecentSearch(updatedRecentSearchIndex);
                }
            }
            log.info("Recent searches cleared successfully for userId={}", userId);
        } catch (IOException e) {
            log.error("Failed to clear recent searches for userId={}", userId, e);

        }
    }
}
