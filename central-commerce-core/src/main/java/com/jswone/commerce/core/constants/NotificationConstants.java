package com.jswone.commerce.core.constants;

public class NotificationConstants {

    public static final String TEAMS = "TEAMS";

    public static final String BUY_AGAIN_CACHE_WARM_UP_SUMMARY_MESSAGE =
            "===== Buy-Again Cache Warm-up Summary =====\n"
                    + "\nTotal Customer IDs Cache Attempted: %s\n"
                    + "\nCustomer IDs successfully cached: %s\n"
                    + "\nTotal Failed Customer IDs: %s\n"
                    + "\nPlease check the Central Commerce Service logs for more details.\n"
                    + "\nNote: Log to search to get list of failed Buy Again Customer IDs - 'Failed buy again cache customer IDs:'";

    public static final String BUY_AGAIN_CT_CACHE_WARM_UP_SUMMARY_MESSAGE =
            "===== Buy-Again CT Cache Warm-up Summary =====\n"
                    + "\nTotal Customer IDs Cache Attempted: %s\n"
                    + "\nCustomer IDs successfully cached: %s\n"
                    + "\nTotal Failed Customer IDs: %s\n"
                    + "\nPlease check the Central Commerce Service logs for more details.\n"
                    + "\nNote: Log to search to get list of failed Buy Again Customer IDs - 'Failed buy again cache customer IDs:'";

    public static final String SEARCH_API_FAILURE_MESSAGE =
            "===== Search API Failure Alert =====\n"
                    + "\nTime: %s\n"
                    + "\nFailure Reason: %s\n"
                    + "\nAPI Payload: %s\n"
                    + "\nPlease check the Central Commerce Service logs for more details.";
}
