package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.model.request.PageMetricRequest;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PageMetricServiceImpl {

    private final ObservationRegistry observationRegistry;

    public boolean getPageLoadMetrics(PageMetricRequest request){
    try {
        Observation.createNotStarted("page.load", observationRegistry)
            .lowCardinalityKeyValue("pageName", request.getPageName())
            .lowCardinalityKeyValue("env", request.getEnv())
            .lowCardinalityKeyValue("version", request.getAppVersion())
            .lowCardinalityKeyValue("device", request.getAppDevice());
        return true;
    }catch (Exception e){
        return false;
    }
    }
}
