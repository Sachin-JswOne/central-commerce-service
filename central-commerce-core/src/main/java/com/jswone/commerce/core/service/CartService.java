package com.jswone.commerce.core.service;

import com.jswone.commons.enums.CartJourneyType;
import com.jswone.commons.enums.CartType;
import com.jswone.commons.graphql.v2.OrdersV2;

public interface CartService {
    OrdersV2 getCartInfo(String customerId, CartType cartType, CartJourneyType cartJourneyType);
}
