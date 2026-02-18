package com.jswone.commerce.core.model.seo;

import java.util.List;

public record CategoryResponse(
                String categoryId,
                String categoryType,
                String categorySlug,
                UrlGroup urls,
                List<ProductResponse> products) {
}