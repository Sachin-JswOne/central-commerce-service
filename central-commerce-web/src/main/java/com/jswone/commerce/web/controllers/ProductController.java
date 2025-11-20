package com.jswone.commerce.web.controllers;

import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.model.request.ProductSkuRequest;
import com.jswone.commerce.core.model.response.SkuInfo;
import com.jswone.commerce.core.service.ProductService;
import com.jswone.commerce.core.util.ApiResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class ProductController implements CentralBaseController {
  private final ProductService productService;

  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  @PostMapping(value = "/product/selector", produces = MediaType.APPLICATION_JSON_VALUE)
  public ApiResponse<SkuInfo> getMatchedSkuDetails(
      @RequestBody ProductSkuRequest productSkuRequest) {
    log.debug("Received request to find SkuInfo :{} ", productSkuRequest.toString());
    SkuInfo skuInfo = productService.getMatchedVariantResponse(productSkuRequest);

    return ApiResponseUtil.createSuccessResponse(skuInfo, HttpStatus.OK);
  }
}
