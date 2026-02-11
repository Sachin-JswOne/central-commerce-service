package com.jswone.commerce.core.util.elastic.queryBuilder;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.json.JsonData;
import org.springframework.stereotype.Component;

@Component
public class TrendingSearchQueryBuilder {

    public Query getTrendingSearchTermQuery(String daysOffSet) {

        return BoolQuery.of(b -> b
                        .filter(
                                TermQuery.of(t -> t
                                        .field("is_eligible")
                                        .value(true)
                                )._toQuery(),
                                TermQuery.of(t -> t
                                        .field("is_barred")
                                        .value(false)
                                )._toQuery(),
                                RangeQuery.of(r -> r
                                        .date(d -> d
                                                .field("timestamp")
                                                .gte("now-".concat(daysOffSet).concat("d"))
                                                .lte("now")
                                        )
                                )._toQuery()
                        )
                )
                ._toQuery();

    }
}
