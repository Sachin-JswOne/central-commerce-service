package com.jswone.commerce.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ElasticPublisherEventTypes {

    PUBLISH_RECENT_SEARCH("PUBLISH_RECENT_SEARCH"),
    CLEAR_RECENT_SEARCH("CLEAR_RECENT_SEARCH"),
    PUBLISH_RECENT_VIEWED("PUBLISH_RECENT_VIEWED"),
    PUBLISH_TRENDING_PRODUCT("PUBLISH_TRENDING_PRODUCT"),
    PUBLISH_TRENDING_CATEGORY("PUBLISH_TRENDING_CATEGORY");


    private final String value;
}
