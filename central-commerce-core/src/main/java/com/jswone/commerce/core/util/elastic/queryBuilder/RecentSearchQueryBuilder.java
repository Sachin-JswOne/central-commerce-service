package com.jswone.commerce.core.util.elastic.queryBuilder;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class RecentSearchQueryBuilder {

    public Query getRecentSearchQuery(String userId) {

        return BoolQuery.of(b -> b
                        .filter(
                                TermQuery.of(t -> t
                                        .field("user_id")
                                        .value(userId)
                                )._toQuery(),
                                TermQuery.of(t -> t
                                        .field("to_be_shown_in_recent")
                                        .value(true)
                                )._toQuery()
                        )
                )
                ._toQuery();

    }

    public Query getRecentSearchQueryGtTimestamp(String userId, String timestamp) {

        return BoolQuery.of(b -> b
                        .filter(
                                TermQuery.of(t -> t
                                        .field("user_id")
                                        .value(userId)
                                )._toQuery(),
                                TermQuery.of(t -> t
                                        .field("to_be_shown_in_recent")
                                        .value(true)
                                )._toQuery(),
                                RangeQuery.of(r -> r
                                        .date(d -> d
                                                .field("timestamp")
                                                .gte(timestamp)
                                        )
                                )._toQuery()
                        )
                )
                ._toQuery();

    }

}
