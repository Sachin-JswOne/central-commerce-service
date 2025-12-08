package com.jswone.commerce.core.service;

import com.jswone.commerce.core.model.BuyAgainResponse;

public interface BuyAgainServiceV2 {

    BuyAgainResponse getRecentPurchasedOrdersList(int offset, int limit);

}
