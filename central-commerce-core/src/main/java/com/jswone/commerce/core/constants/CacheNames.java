package com.jswone.commerce.core.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CacheNames {
        public static final String CENTRAL_COMMERCE_SERVICE_CACHE_PREFIX = "central-commerce:";

        public static final String BUY_AGAIN_PRODUCTS_CACHE_PREFIX = CENTRAL_COMMERCE_SERVICE_CACHE_PREFIX
                        + "buy_again_products_ct:customer";

        public static final String BUY_AGAIN_PRODUCTS_CACHE_PREFIX_V2 = CENTRAL_COMMERCE_SERVICE_CACHE_PREFIX
                        + "buy_again_products:customer";

        public static final String CLEAR_RECENT_SEARCHES_CACHE_PREFIX = CENTRAL_COMMERCE_SERVICE_CACHE_PREFIX
                        + "clear_recent_searches:customer";

        public static final String SEO_CATEGORY_LOCATIONS = CENTRAL_COMMERCE_SERVICE_CACHE_PREFIX
                        + "seo:category_locations";

        public static final String SEO_PRODUCT_TYPES = CENTRAL_COMMERCE_SERVICE_CACHE_PREFIX
                        + "seo:product_types";
}
