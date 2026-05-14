package com.jswone.commerce.web.controllers;

import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.service.PurchasedSkuService;
import com.jswone.commerce.core.util.ApiResponseUtil;
import com.jswone.commons.util.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/customer")
public class CustomerController {

    private final PurchasedSkuService purchasedSkuService;

    public CustomerController(PurchasedSkuService purchasedSkuService) {
        this.purchasedSkuService = purchasedSkuService;
    }

    @GetMapping("/transacting")
    public ApiResponse<Boolean> transactingCustomer() {
        String customerId = JwtTokenUtil.getUserIdForSession();
        log.info("transacting customer for customerId={}", customerId);
        boolean isTransactingCustomer = purchasedSkuService
                .checkTransactingCustomer(customerId);

        return ApiResponseUtil.createSuccessResponse(isTransactingCustomer,
                HttpStatus.OK);
    }
}