package com.jswone.commerce.core.model.seo;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class UrlMeta {
    private final String url;
    private final Instant lastMod;
    private final double priority;
}
