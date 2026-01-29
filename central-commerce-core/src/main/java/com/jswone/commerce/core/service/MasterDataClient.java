package com.jswone.commerce.core.service;

import com.jswone.commerce.core.model.masters.Data;

import java.util.List;
import java.util.Map;

public interface MasterDataClient {

    Map<String, Data> fetchProductDetails(List<String> productMMIDs);

}
