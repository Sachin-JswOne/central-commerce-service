package com.jswone.commerce.core.template;

import com.jswone.commerce.core.config.SeoUrlProperties;
import com.jswone.commerce.core.enums.seo.CategoryType;
import com.jswone.commerce.core.model.seo.SeoContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UrlTemplateResolver {

    private final SeoUrlProperties props;

    public String categoryBase(SeoContext ctx) {
        return ctx.getCategoryType() == CategoryType.BRAND
                ? props.getBrand().getBase()
                : props.getCategory().getBase();
    }

    public String categoryLocation(SeoContext ctx) {
        return ctx.getCategoryType() == CategoryType.BRAND
                ? props.getBrand().getLocation()
                : props.getCategory().getLocation();
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
