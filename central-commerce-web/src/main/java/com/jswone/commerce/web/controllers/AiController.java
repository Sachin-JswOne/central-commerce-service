package com.jswone.commerce.web.controllers;

import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.model.request.AiInvokeRequest;
import com.jswone.commerce.core.model.response.AiInvokeResponse;
import com.jswone.commerce.core.service.AiService;
import com.jswone.commerce.core.util.ApiResponseUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping(value = "/ai/invoke", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<AiInvokeResponse> invoke(@Valid @RequestBody AiInvokeRequest request) {
        log.debug("Received AI invoke request: {}", request);
        AiInvokeResponse response = aiService.invoke(request);
        return ApiResponseUtil.createSuccessResponse(response, HttpStatus.OK);
    }

    @PostMapping(value = "/ai/invoke-business-classification", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<AiInvokeResponse> invokeBusinessClassification(@Valid @RequestBody AiInvokeRequest request) {
        log.debug("Received AI business classification request: {}", request);
        AiInvokeResponse response = aiService.invokeBusinessClassification(request);
        return ApiResponseUtil.createSuccessResponse(response, HttpStatus.OK);
    }
}
