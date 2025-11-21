package com.jswone.commerce.core.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CacheNames {
    public static final String CENTRAL_COMMERCE_SERVICE_CACHE_PREFIX = "central-commerce:";

    public static final String BUY_AGAIN_PRODUCTS = CENTRAL_COMMERCE_SERVICE_CACHE_PREFIX + "buy_again_products:customer";
}
