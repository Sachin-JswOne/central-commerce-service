package com.jswone.commerce.core.model.request;

import lombok.Data;

@Data
public class ApiMetricRequest {

    private String httpMethodName;

    private int httpStatus;

    private String httpPath;

    private String loadTime;

    private String env;

    private String appVersion;

    private String appDevice;
}
