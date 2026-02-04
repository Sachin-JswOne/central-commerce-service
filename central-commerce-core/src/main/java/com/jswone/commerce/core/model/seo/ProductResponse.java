package com.jswone.commerce.core.model.seo;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ProductResponse {
    private final String productId;
    private final String productSlug;
    private final UrlGroup urls;
    private final List<VariantResponse> variants;
}

