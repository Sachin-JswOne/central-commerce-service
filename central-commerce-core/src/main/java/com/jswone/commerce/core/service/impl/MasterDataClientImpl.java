package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.model.masters.Data;
import com.jswone.commerce.core.model.masters.ProductDetailBulkResponse;
import com.jswone.commerce.core.service.MasterDataClient;
import com.jswone.commerce.core.util.RestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.jswone.commerce.core.constants.RestConstants.CLIENT_ID;
import static com.jswone.commerce.core.constants.RestConstants.X_API_KEY;

@Service
@Slf4j
public class MasterDataClientImpl implements MasterDataClient {

    private final RestUtil restUtil;

    private final CommerceValueConfig commerceValueConfig;

    public MasterDataClientImpl(RestUtil restUtil, CommerceValueConfig commerceValueConfig) {
        this.restUtil = restUtil;
        this.commerceValueConfig = commerceValueConfig;
    }

    @Override
    public Map<String, Data> fetchProductDetails(List<String> productMMIDs) {

        if (productMMIDs == null || productMMIDs.isEmpty()) {
            throw new IllegalArgumentException("productMMID cannot be null or empty");
        }

        return getProductDetailsFromMasters(productMMIDs);
    }

    public Map<String, Data> getProductDetailsFromMasters(List<String> productMMIdList) {

        try {
            String url = commerceValueConfig.getMasterDataServiceBaseUrl() + commerceValueConfig.getBulkMasterDataProductMMIDEndpoint();

            Map<String, String> headers = Map.of(
                    X_API_KEY, commerceValueConfig.getBulkMasterDataProductMMIDApiKey(),
                    CLIENT_ID, commerceValueConfig.getBulkMasterDataProductMMIDClientId(),
                    "Content-Type", "application/json"
            );

            Map<String, List<String>> body = Map.of("mmids", productMMIdList);

            ResponseEntity<ProductDetailBulkResponse> response = restUtil.makeRestCall(
                    url, body, HttpMethod.POST, ProductDetailBulkResponse.class, headers);

            ProductDetailBulkResponse respBody = response.getBody();

            if (respBody == null || respBody.getData() == null) {
                throw new RuntimeException("Empty response received from master data service");
            }

            return respBody.getData()
                    .stream()
                    .collect(Collectors.toMap(Data::getMmid, d -> d));

        } catch (HttpClientErrorException e) {
            log.error("Master data error: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve material master data", e);
        }
    }
}
