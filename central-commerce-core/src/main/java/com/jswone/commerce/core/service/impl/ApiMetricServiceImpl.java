package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.model.request.ApiMetricRequest;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class ApiMetricServiceImpl {

    private final ObservationRegistry observationRegistry;

    public boolean getApiMetrics(ApiMetricRequest request) {

        try {
            Observation.createNotStarted("api.request", observationRegistry).
                    lowCardinalityKeyValue("method", request.getHttpMethodName()).
                    lowCardinalityKeyValue("status", String.valueOf(request.getHttpStatus())).
                    lowCardinalityKeyValue("env", request.getEnv()).
                    lowCardinalityKeyValue("version", request.getAppVersion()).
                    lowCardinalityKeyValue("device", request.getAppDevice());
            return true;
        }catch (Exception e){
            return false;
        }
    }
}