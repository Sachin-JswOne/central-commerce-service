package com.jswone.commerce.core.model.elastic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    private String eventId;
    private String eventType;
    private Map<String, Object> payload;
}
