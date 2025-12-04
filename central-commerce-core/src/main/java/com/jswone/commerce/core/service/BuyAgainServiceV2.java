package com.jswone.commerce.core.service;

import com.jswone.commerce.core.model.DistributedBuyAgainResponse;

public interface BuyAgainServiceV2 {

    DistributedBuyAgainResponse getRecentPurchasedDistributedOrdersList(int offset, int limit);

}
