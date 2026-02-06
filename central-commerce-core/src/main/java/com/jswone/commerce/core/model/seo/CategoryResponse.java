package com.jswone.commerce.core.model.seo;

import com.jswone.commerce.core.enums.seo.CategoryType;

import java.util.List;

public record CategoryResponse(
        String categoryId,
        CategoryType categoryType,
        String categorySlug,
        UrlGroup urls,
        List<ProductResponse> products
) {}