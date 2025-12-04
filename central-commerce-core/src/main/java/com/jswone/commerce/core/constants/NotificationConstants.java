package com.jswone.commerce.core.constants;

public class NotificationConstants {

    public static final String TEAMS = "TEAMS";

    public static final String BUY_AGAIN_CACHE_WARM_UP_SUMMARY_MESSAGE =
            "===== Buy-Again Cache Warm-up Summary =====\n"
                    + "\nRetries Attempted: %s\n"
                    + "\nTotal Failed Customer IDs: %s\n"
                    + "\nCustomer IDs that could not be cached:\n%s\n"
                    + "\nPlease check the Central Commerce Service logs for more details.";
}
