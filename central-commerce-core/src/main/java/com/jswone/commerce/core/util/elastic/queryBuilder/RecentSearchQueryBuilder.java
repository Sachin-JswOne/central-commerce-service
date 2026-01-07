package com.jswone.commerce.core.util.elastic.queryBuilder;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import org.springframework.stereotype.Component;

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

}
