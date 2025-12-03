package com.jswone.commerce.core.constants;

public class NotificationConstants {

    public static final String TEAMS = "TEAMS";

    public static final String BUY_AGAIN_CACHE_WARM_UP_FAILURE_MESSAGE =
            "BUY-AGAIN Cache Warm-up Failed "
                    + "Retries attempted: %s. "
                    + "Total failed customer IDs: %s. "
                    + "The following customer IDs could not be cached even after all retries: %s. "
                    + "Please check the service logs for more details";

}
