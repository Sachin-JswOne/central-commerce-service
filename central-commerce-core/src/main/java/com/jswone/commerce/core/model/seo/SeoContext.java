package com.jswone.commerce.core.model.seo;

import com.jswone.commerce.core.enums.seo.CategoryType;
import com.jswone.commerce.core.enums.seo.SeoEntityType;
import com.jswone.commerce.core.enums.seo.SeoOperationType;
import com.jswone.commerce.core.enums.seo.SeoPageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

@Getter
@Builder
@ToString
@AllArgsConstructor
public class SeoContext {
    private final SeoEntityType entityType;
    private final SeoPageType pageType;
    private final CategoryType categoryType;
    private final String slug;
    private final String location;
    private final String variantMmid;
    private final String categoryId;
    private final String productId;
    private final SeoOperationType operationType;
    private final Instant lastModifiedAt;}
