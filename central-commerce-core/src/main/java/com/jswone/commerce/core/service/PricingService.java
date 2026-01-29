package com.jswone.commerce.core.service;

import com.jswone.commerce.core.model.pricing.LineItemPrice;
import com.jswone.commerce.core.model.pricing.PriceRequest;

import java.util.List;

public interface PricingService {
    List<LineItemPrice> getPrice(PriceRequest priceRequest);
}
