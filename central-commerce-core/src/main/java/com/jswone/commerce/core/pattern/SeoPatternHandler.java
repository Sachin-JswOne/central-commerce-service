package com.jswone.commerce.core.pattern;

import com.jswone.commerce.core.model.seo.SeoContext;
import com.jswone.commerce.core.model.seo.SeoData;
import com.jswone.commerce.core.model.seo.SeoMeta;
import com.jswone.commerce.core.model.seo.UrlMeta;

public interface SeoPatternHandler {

    UrlMeta generateUrl(SeoContext context);

    SeoMeta generateMeta(SeoContext context, SeoData data);
}
