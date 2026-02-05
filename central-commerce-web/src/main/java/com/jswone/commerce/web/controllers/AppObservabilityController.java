package com.jswone.commerce.web.controllers;

import com.jswone.commerce.core.model.request.ApiMetricRequest;
import com.jswone.commerce.core.model.request.PageMetricRequest;
import com.jswone.commerce.core.service.impl.ApiMetricServiceImpl;
import com.jswone.commerce.core.service.impl.PageMetricServiceImpl;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AppObservabilityController implements CentralBaseController{

    private final PageMetricServiceImpl pageMetricService;
    private final ApiMetricServiceImpl apiMetricService;

    @PostMapping(value = "/metrics/pageload-metrics")
    public ResponseEntity<Boolean> getPageLoadMetrics(@Valid @RequestBody PageMetricRequest pageMetricRequest){
        boolean success = pageMetricService.getPageLoadMetrics(pageMetricRequest);
        return ResponseEntity.ok(success);
    }

    @PostMapping(value = "/metrics/api-metrics")
    public ResponseEntity<Boolean> getApiMetrics(@Valid @RequestBody ApiMetricRequest apiMetricRequest){
        boolean success = apiMetricService.getApiMetrics(apiMetricRequest);
        return ResponseEntity.ok(success);
    }
}
