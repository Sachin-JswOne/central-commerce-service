package com.jswone.commerce.core.service;

import com.jswone.commons.enums.CartJourneyType;
import com.jswone.commons.enums.CartType;
import com.jswone.commons.graphql.v2.OrdersV2;

import java.util.Set;

public interface CartService {
    OrdersV2 getCartInfo(String customerId, CartType cartType, CartJourneyType cartJourneyType);
    Set<String> getProductMMID(OrdersV2 cart);
}
