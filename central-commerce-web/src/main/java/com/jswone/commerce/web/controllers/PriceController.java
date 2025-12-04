package com.jswone.commerce.web.controllers;

import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.model.pricing.LineItemPrice;
import com.jswone.commerce.core.model.pricing.PriceRequest;
import com.jswone.commerce.core.service.PricingService;
import com.jswone.commerce.core.util.ApiResponseUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class PriceController implements CentralBaseController{
    private final PricingService pricingService;

    public PriceController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @PostMapping(value = "/line-item/price", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<List<LineItemPrice>> getPrice(@Valid @RequestBody PriceRequest priceRequest) {
        log.debug("Received request to call price request :{} ", priceRequest.toString());
        List<LineItemPrice> response = pricingService.getPrice(priceRequest);

        return ApiResponseUtil.createSuccessResponse(response, HttpStatus.OK);
    }
}
