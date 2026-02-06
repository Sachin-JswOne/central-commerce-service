package com.jswone.commerce.core.resolver;

import com.jswone.commerce.core.model.seo.SeoContext;

import java.util.Map;

/**
 * Resolves incoming SEO-friendly URLs into SeoContext.
 *
 * Responsibilities:
 * - Interpret URL structure
 * - Extract slug, location, variant
 * - Decide entity type & page type
 *
 * Controllers must NOT build SeoContext directly.
 */
public interface SeoContextResolver {
    SeoContext resolve(String path, Map<String, String> pathVariables);
}
