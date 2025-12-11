package com.jswone.commerce.core.service;

import com.jswone.commerce.core.entity.PurchasedSku;

import java.util.List;

public interface PurchasedSkuService {
    List<PurchasedSku> fetchRecentlyPurchasedSku(String customerId);

    List<PurchasedSku> fetchRecentlyPurchasedSkuForAllCustomers(int offset, int limit);
}
