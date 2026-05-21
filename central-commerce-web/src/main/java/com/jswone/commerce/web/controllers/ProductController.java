package com.jswone.commerce.web.controllers;

import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.model.centralCatalogue.ProductSlug;
import com.jswone.commerce.core.model.request.ProductSkuRequest;
import com.jswone.commerce.core.model.response.SkuInfo;
import com.jswone.commerce.core.service.ProductService;
import com.jswone.commerce.core.util.ApiResponseUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping(value = "/product/selector", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@permissionValidator.isAnonymousUser(principal) or " +
                  "@permissionValidator.hasPermissionForResource(principal.permissions, 'REQUIREMENTS_CART', '00010000') ")
    public ApiResponse<SkuInfo> getMatchedSkuDetails(@Valid @RequestBody ProductSkuRequest productSkuRequest) {
        log.debug("Received request to find SkuInfo :{} ", productSkuRequest.toString());
        SkuInfo skuInfo = productService.getMatchedVariantResponse(productSkuRequest);

        return ApiResponseUtil.createSuccessResponse(skuInfo, HttpStatus.OK);
    }


    @GetMapping(value = "/product/slug/{*slugPath}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@permissionValidator.isAnonymousUser(principal) or " +
            "@permissionValidator.hasPermissionForResource(principal.permissions, 'REQUIREMENTS_CART', '00010000') ")
    public ApiResponse<ProductSlug> getProductFromSlug(
            @PathVariable String slugPath,
            @RequestParam(defaultValue = "msme") String storeFront) {
        log.debug("Received request to find product slug :{} ", slugPath);
        ProductSlug productSlug = productService.getProductFromSlug(slugPath, storeFront);

        return ApiResponseUtil.createSuccessResponse(productSlug, HttpStatus.OK);
    }
}
