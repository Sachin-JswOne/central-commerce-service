package com.jswone.commerce.core.model.seo;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class VariantResponse {
    private final String variantMmid;
    private final UrlGroup urls;
}
