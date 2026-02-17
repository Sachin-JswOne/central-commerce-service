package com.jswone.commerce.core.model.seo;

import lombok.Getter;
import lombok.Builder;
import lombok.AllArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class UrlMeta {
    private final String url;
    private final Instant lastMod;
    private final double priority;
}
