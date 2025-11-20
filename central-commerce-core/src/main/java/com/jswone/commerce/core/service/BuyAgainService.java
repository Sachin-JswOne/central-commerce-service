package com.jswone.commerce.core.service;

import com.jswone.commerce.core.model.DistributedBuyAgainResponse;

public interface BuyAgainService {

    DistributedBuyAgainResponse getRecentPurchasedDistributedOrdersList(int offset, int limit);

    void loadAllBuyAgainProductsForCustomersIntoCache();

}
