package com.jswone.commerce.web.controllers;

import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.model.BuyAgainResponse;
import com.jswone.commerce.core.service.BuyAgainService;
import com.jswone.commerce.core.service.BuyAgainServiceV2;
import com.jswone.commerce.core.util.ApiResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class BuyAgainController implements CentralBaseController {

    private final BuyAgainServiceV2 buyAgainServiceV2;

    private final BuyAgainService buyAgainService;

    private final CommerceValueConfig commerceValueConfig;

    public BuyAgainController(BuyAgainServiceV2 buyAgainServiceV2, BuyAgainService buyAgainService, CommerceValueConfig commerceValueConfig) {
        this.buyAgainServiceV2 = buyAgainServiceV2;
        this.buyAgainService = buyAgainService;
        this.commerceValueConfig = commerceValueConfig;
    }

    @GetMapping(value = "/buy-again-list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<BuyAgainResponse> getRecentPurchasedList(
            @RequestParam(defaultValue = "0", name = "offset") Integer offset,
            @RequestParam(defaultValue = "50", name = "limit") Integer limit) {

        if (commerceValueConfig.isCentralCatalogueServiceEnabled()) {
            log.info("Central Catalogue Service is enabled. Using buy again service V2.");
            return ApiResponseUtil.createSuccessResponse(buyAgainServiceV2.getRecentPurchasedOrdersList(offset, limit),
                    HttpStatus.OK);
        }
        log.info("Central Catalogue Service is disabled. Using legacy buy again service.");
        return ApiResponseUtil.createSuccessResponse(buyAgainService.getRecentPurchasedDistributedOrdersList(offset, limit),
                HttpStatus.OK);
    }
}
