package com.jswone.commerce.core.model.request;

import lombok.Data;

@Data
public class PageMetricRequest {

    private String pageName;

    private float loadTime;

    private String env;

    private String appVersion;

    private String appDevice;

}
