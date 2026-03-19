package com.jswone.commerce.web.controllers;

import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.model.request.UomConvertRequest;
import com.jswone.commerce.core.model.response.search.UomConvertResponse;
import com.jswone.commerce.core.service.UomConvertService;
import com.jswone.commerce.core.util.ApiResponseUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class MeasurementController {

    private final UomConvertService uomConvertService;

    public MeasurementController(UomConvertService uomConvertService) {
        this.uomConvertService = uomConvertService;
    }

    @PostMapping("/uom-convert")
    public ApiResponse<UomConvertResponse> convertUom(@RequestBody @Valid UomConvertRequest uomConvertRequest) {
        return ApiResponseUtil.createSuccessResponse(uomConvertService.convertUom(uomConvertRequest.getUomRequests()),
                HttpStatus.OK);
    }
}
