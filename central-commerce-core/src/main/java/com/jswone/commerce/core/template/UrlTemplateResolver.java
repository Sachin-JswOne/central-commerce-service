package com.jswone.commerce.core.template;

import com.jswone.commerce.core.config.SeoUrlProperties;
import com.jswone.commerce.core.constants.SeoConstants;
import com.jswone.commerce.core.model.seo.SeoContext;
import com.jswone.commerce.core.util.CatalogueUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UrlTemplateResolver {

    private final SeoUrlProperties props;

    public String categoryBase(SeoContext ctx) {
        if (SeoConstants.CATEGORY_TYPE_ALL_PRODUCTS.equalsIgnoreCase(ctx.getCategoryType())) {
            return props.getCategory().getBase();
        }
        String prefix = CatalogueUtil.getSeoUrlCategoryPrefix(ctx.getCategoryType());
        return "/" + prefix + "/{slug}";
    }

    public String categoryLocation(SeoContext ctx) {
        if (SeoConstants.CATEGORY_TYPE_ALL_PRODUCTS.equalsIgnoreCase(ctx.getCategoryType())) {
            return props.getCategory().getLocation();
        }
        String prefix = CatalogueUtil.getSeoUrlCategoryPrefix(ctx.getCategoryType());
        return "/" + prefix + "/{location}/{slug}";
    }

    public String productBase() {
        return props.getProduct().getBase();
    }

    public String productLocation() {
        return props.getProduct().getLocation();
    }

    public String variantLocation() {
        return props.getProduct().getVariant();
    }
}
