package com.jswone.commerce.core.model.seo;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.util.Map;

@Getter
@AllArgsConstructor
public class UrlGroup {
    private final UrlMeta base;
    private final Map<String, UrlMeta> locations;
}
