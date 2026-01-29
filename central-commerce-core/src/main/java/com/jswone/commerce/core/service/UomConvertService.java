package com.jswone.commerce.core.service;

import com.jswone.commerce.core.model.PurchasedUom;
import com.jswone.commerce.core.model.response.search.UomConvertResponse;

import java.util.List;

public interface UomConvertService {

    UomConvertResponse convertUom(List<PurchasedUom> purchasedUom);
}
