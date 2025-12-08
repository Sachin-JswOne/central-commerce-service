package com.jswone.commerce.core.service;

import com.jswone.commerce.core.model.BuyAgainResponse;

public interface BuyAgainService {

    BuyAgainResponse getRecentPurchasedDistributedOrdersList(int offset, int limit);
}
