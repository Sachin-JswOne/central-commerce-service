package com.jswone.commerce.core.factory;

import com.jswone.commerce.core.model.seo.SeoContext;
import com.jswone.commerce.core.pattern.SeoPatternHandler;
import com.jswone.commerce.core.pattern.impl.DefaultSeoPatternHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Factory responsible for resolving the correct SEO pattern handler
 * for a given SeoContext.
 *
 */
@Component
@RequiredArgsConstructor
public class SeoPatternFactory {

    private final DefaultSeoPatternHandler defaultSeoPatternHandler;

    /**
     * Resolves the appropriate SeoPatternHandler.
     *
     * Currently:
     * - Always returns DefaultSeoPatternHandler
     *
     * Future:
     * - Can switch based on context
     * - Can wrap with decorators
     */
    public SeoPatternHandler resolve(SeoContext context) {
        return defaultSeoPatternHandler;
    }
}