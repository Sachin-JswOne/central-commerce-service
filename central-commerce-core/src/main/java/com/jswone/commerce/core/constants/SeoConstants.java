package com.jswone.commerce.core.constants;

/**
 * Constants for SEO and Sitemap generation
 */
public final class SeoConstants {

    private SeoConstants() {
        // Prevent instantiation
    }

    // ============ Product Attributes ============
    public static final String ATTR_SLUG = "slug";
    public static final String ATTR_PRODUCT_TITLE = "product_title";
    public static final String ATTR_META_IMAGE = "meta_image";
    public static final String ATTR_META_TITLE = "meta_title";
    public static final String ATTR_META_DESCRIPTION = "meta_description";
    public static final String ATTR_LOCATION = "location";
    public static final String ATTR_MMID = "mmid";

    // ============ Category Types ============
    public static final String CATEGORY_TYPE_BRANDS = "Brands";

    // ============ Locations ============
    public static final String LOCATION_ALL = "all";

    // ============ Locale & Storefront ============
    public static final String LOCALE_EN_US = "en-US";
    public static final String STOREFRONT_MSME = "msme";

    // ============ OpenGraph Types ============
    public static final String OG_TYPE_WEBSITE = "website";
    public static final String OG_TYPE_PRODUCT = "product";

    // ============ URL Path Segments ============
    public static final String URL_SEGMENT_CATEGORY = "category";
    public static final String URL_SEGMENT_PRODUCT_DETAIL = "product-detail";

    // ============ Template Placeholders ============
    public static final String PLACEHOLDER_CATEGORY_NAME = "{categoryName}";
    public static final String PLACEHOLDER_PRODUCT_NAME = "{productName}";
    public static final String PLACEHOLDER_LOCATION = "{location}";
    public static final String PLACEHOLDER_SLUG = "{slug}";
    public static final String PLACEHOLDER_VARIANT_ATTRIBUTES = "{variantAttributes}";
    public static final String PLACEHOLDER_VARIANT_MMID = "{variantMmid}";

    // ============ Default Values ============
    public static final String DEFAULT_LOCATION = "India";
    public static final String DEFAULT_SEPARATOR = " | ";
    public static final String DEFAULT_BRAND_NAME = "JSW One MSME";

    // ============ Cache Keys ============
    public static final String CACHE_PREFIX_SEO_LOCATION = "seo:location:";

    // ============ Sitemap Generation ============
    public static final int SITEMAP_DEFAULT_PAGE_SIZE = 100;
    public static final int SITEMAP_MAX_URLS_PER_FILE = 50000;
    public static final String SITEMAP_CHANGEFREQ_DAILY = "daily";
    public static final String SITEMAP_PRIORITY_HIGH = "1.0";
    public static final String SITEMAP_PRIORITY_MEDIUM = "0.8";
    public static final String SITEMAP_PRIORITY_LOW = "0.5";
}
