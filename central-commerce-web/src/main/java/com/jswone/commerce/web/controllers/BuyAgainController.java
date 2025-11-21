package com.jswone.commerce.web.controllers;

import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.model.DistributedBuyAgainResponse;
import com.jswone.commerce.core.service.BuyAgainService;
import com.jswone.commerce.core.util.ApiResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class BuyAgainController implements CentralBaseController {

    private final BuyAgainService buyAgainService;

    public BuyAgainController(BuyAgainService buyAgainService) {
        this.buyAgainService = buyAgainService;
    }

    @GetMapping(value = "/buy-again/listing/v2", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<DistributedBuyAgainResponse> getRecentPurchasedList(
            @RequestParam(defaultValue = "0", name = "offset") Integer offset,
            @RequestParam(defaultValue = "50", name = "limit") Integer limit) {
        return ApiResponseUtil.createSuccessResponse(buyAgainService.getRecentPurchasedDistributedOrdersList(offset, limit),
                HttpStatus.OK);
    }

    @PostMapping("/buy-again/cache/warmup")
    public ApiResponse<String> buyAgainProductsWarmupCache() {
        try {
            buyAgainService.loadAllBuyAgainProductsForCustomersIntoCache();
            return ApiResponseUtil.createSuccessResponse("Cache warm-up of buy again products for all customers completed!",
                    HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error during buy-again product cache warmup", e);
            return ApiResponseUtil.createErrorResponse("Cache warm-up of buy again products for all customers failed: "
                    + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
