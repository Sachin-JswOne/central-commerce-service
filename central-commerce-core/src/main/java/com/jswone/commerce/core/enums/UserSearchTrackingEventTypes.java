package com.jswone.commerce.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserSearchTrackingEventTypes {

    RECENT_SEARCH("RECENT_SEARCH"),
    USER_SEARCH_LOGS("USER_SEARCH_LOGS");

    private final String value;
}
