package com.jswone.commerce.core.model.seo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class ProductResponse {
    private final String productId;
    private final String productSlug;
    private final UrlGroup urls;
    private final Map<String, UrlMeta> stateUrls;
    private final Map<String, UrlMeta> cityUrls;
    private final List<VariantResponse> variants;
}
