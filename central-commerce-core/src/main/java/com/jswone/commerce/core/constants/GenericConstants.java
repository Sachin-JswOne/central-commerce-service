package com.jswone.commerce.core.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GenericConstants {
    public static final String COMMA = ",";
    public static final String CENTRAL_CATALOGUE_SEARCH = "central_catalogue_search";
    public static final String CENTRAL_CATALOGUE_BULK_MMID = "central_catalogue_bulk_mmid";
    public static final String CENTRAL_CATALOGUE_ADMIN_BULK_TYPEID = "central_catalogue_admin_bulk_typeid";
    public static final String CENTRAL_CATALOGUE_PRICING = "central_catalogue_pricing";
    public static final char[] ALPHABETS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGJKLMNPRSTUVWXYZ0123456789".toCharArray();
    public static final String PRICE_FETCH_DISPLAY_ERROR_MESSAGE =
            "Product unavailable. Please try again later.";
    public static final String IS_FREIGHT_CHARGE_AVAILABLE = "isFreightChargeAvailable";
    public static final String FREIGHT_CHARGE = "freightCharge";
    public static final String EMPTY_STRING = "";
    public static final String MMID_SUFFIX = "-10000000";
    public static final int BULK_IMAGE_CHUNK_SIZE = 100;
    public static final String CENTRAL_CATALOGUE_PRODUCT_SLUG = "central_catalogue_product_slug";
    public static final String CENTRAL_CATALOGUE_MIN_SUFFIX = "_min";
    public static final String CENTRAL_CATALOGUE_MAX_SUFFIX = "_max";
    public static final String CENTRAL_CATALOGUE_CUSTOM_ATTRIBUTE_KEY = "key";
    public static final String CENTRAL_CATALOGUE_CUSTOM_ATTRIBUTE_VALUE = "value";
    public static final String CENTRAL_CATALOGUE_STANDARD_ATTRIBUTE_NAME_KEY = "name";
    public static final String CENTRAL_CATALOGUE_STANDARD_ATTRIBUTE_LABEL_KEY = "ui_label";
    public static final String CENTRAL_CATALOGUE_PDP_IDENTIFIER = "TABLE_WITH_QUANTITY_FIELDS";
    public static final String CENTRAL_CATALOGUE_STANDARD_ATTRIBUTE_UNIT_KEY = "unit_key";
}
