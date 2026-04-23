package com.jswone.commerce.core.constants;

import java.util.Arrays;
import java.util.List;

import lombok.experimental.UtilityClass;

@UtilityClass
public class JWTConstants {
    public static final String SHA_512 = "SHA-512";
    public static final String X_API_KEY = "X-API-KEY";
    public static final String HYPHEN = "-";
    public static final String TOKEN_EXPIRE_MESSAGE = "Token Expired : cannot be reused";
    public static final String INVALID_TOKEN_MESSAGE = "Invalid token, claims expected";
    public static final String TOKEN_NOT_PRESENT_MESSAGE = "Auth token not present";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String SESSION_ID_HEADER = "X-Session-Id";
    public static final String CORRELATION_ID_CLAIM = "x-correlation-id";
    public static final List<String> excludeUrlPatterns =
            Arrays.asList(
                    "/cart/notification/**",
                    "/import/cart",
                    "/cart/dlq/cart-opportunity",
                    "/**/actuator/**",
                    "/**/sl/**");

    public static final String USER_ID_CLAIM = "userId";
    public static final String SF_ID_CLAIM = "sfCustomerId";
    public static final String USER_TYPE_CLAIM = "userType";
    public static final String GUEST_USER_TYPE = "G";
    public static final String SESSION_ID_COOKIE_NAME = "jsw_session_id";

}
