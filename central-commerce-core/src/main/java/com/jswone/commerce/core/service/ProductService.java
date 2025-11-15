package com.jswone.commerce.core.service;

import com.jswone.commerce.core.model.request.ProductSkuRequest;
import com.jswone.commerce.core.model.response.SkuInfo;

public interface ProductService {
  SkuInfo getMatchedVariantResponse(ProductSkuRequest productSkuRequest);
}
