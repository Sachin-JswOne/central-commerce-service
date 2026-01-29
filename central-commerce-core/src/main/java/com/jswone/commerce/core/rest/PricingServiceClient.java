package com.jswone.commerce.core.rest;

import com.jswone.commons.pricing.PricingServiceResponse;
import com.jswone.commons.pricing.ProductPricingRequest;

public interface PricingServiceClient {
    PricingServiceResponse callPricingService(ProductPricingRequest productPricingRequest);
}
