package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.accountMaster.CustomerMouData;
import com.jswone.commerce.core.model.accountMaster.CustomerMouRequest;
import com.jswone.commerce.core.model.accountMaster.CustomerMouResponse;
import com.jswone.commerce.core.service.AccountMasterService;
import com.jswone.commerce.core.util.RestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.jswone.commerce.core.constants.RestConstants.X_API_KEY;

@Slf4j
@Service
public class AccountMasterServiceImpl implements AccountMasterService {

    private final CommerceValueConfig commerceValueConfig;
    private final RestUtil restUtil;


    public AccountMasterServiceImpl(CommerceValueConfig commerceValueConfig, RestUtil restUtil) {
        this.commerceValueConfig = commerceValueConfig;
        this.restUtil = restUtil;
    }


    @Override
    public List<CustomerMouData> getCustomerMouDetails(String gstIn, String financialYear) {
        log.info("Fetching MOU customer details from Account Master for GST [{}], FY [{}]", gstIn, financialYear);

        try {
            String mouEndPoint = commerceValueConfig.getAccountMasterServiceBaseUrl()
                    + commerceValueConfig.getAccountMasterServiceMouEndpoint();

            String url = String.format("%s?gstin=%s&financialYear=%s&mouDisplay=true", mouEndPoint, gstIn, financialYear);

            Map<String, String> headers = Map.of(X_API_KEY, commerceValueConfig.getAccountMasterServiceApiKey());

            ResponseEntity<CustomerMouResponse> response =
                    restUtil.makeRestCall(
                            url,
                            null,
                            HttpMethod.GET,
                            CustomerMouResponse.class,
                            headers
                    );

            CustomerMouResponse responseBody = getCustomerMouResponse(response);

            if (responseBody.getData() == null || responseBody.getData().isEmpty()) {
                log.warn("Account Master Customer MOU API returned success but no data for GST [{}], FY [{}]",
                        gstIn, financialYear);
                return Collections.emptyList();
            }

            log.info("Account Master Customer MOU GET API call successful. Records count={}",
                    responseBody.getData().size());

            return responseBody.getData();

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("HttpClientErrorException while calling Account Master Customer MOU GET API: {}",
                    ex.getMessage(), ex);
            throw new CentralCommerceServiceException(
                    String.format("HttpClientErrorException while calling Account Master Customer MOU GET API: %s",
                            ex.getMessage()), HttpStatus.valueOf(ex.getStatusCode().value()));
        } catch (Exception ex) {
            log.error("Exception occurred while calling while calling Account Master Customer MOU GET API: {}", ex.getMessage(), ex);
            throw new CentralCommerceServiceException(
                    String.format("Exception occurred while calling Account Master Customer MOU GET API: %s",
                            HttpStatus.INTERNAL_SERVER_ERROR)
            );
        }
    }

    private CustomerMouResponse getCustomerMouResponse(ResponseEntity<CustomerMouResponse> response) {
        CustomerMouResponse responseBody = response.getBody();

        if (responseBody == null) {
            throw new CentralCommerceServiceException("Account Master Customer MOU API returned empty response body",
                    (HttpStatus) response.getStatusCode());
        }

        if (Boolean.FALSE.equals(responseBody.getSuccess())) {
            throw new CentralCommerceServiceException("Account Master Customer MOU API returned failure: "
                    + responseBody.getMessage(), (HttpStatus) response.getStatusCode());
        }
        return responseBody;
    }

    @Override
    public List<CustomerMouData> getBulkCustomerMouDetails(List<CustomerMouRequest> customerMouBulkRequest) {
        log.info("Fetching MOU customer details from Account Master for request list={}", customerMouBulkRequest);

        try {
            String url = commerceValueConfig.getAccountMasterServiceBaseUrl()
                    + commerceValueConfig.getAccountMasterServiceMouEndpoint() + "/fetch-mou-list";

            Map<String, String> headers = Map.of(
                    X_API_KEY, commerceValueConfig.getAccountMasterServiceApiKey(),
                    "Content-Type", "application/json"
            );

            Map<String, Object> requestBody = Map.of("customerMouRequest", customerMouBulkRequest);

            ResponseEntity<CustomerMouResponse> response =
                    restUtil.makeRestCall(
                            url,
                            requestBody,
                            HttpMethod.POST,
                            CustomerMouResponse.class,
                            headers
                    );

            CustomerMouResponse responseBody = getCustomerMouResponse(response);

            if (responseBody.getData() == null || responseBody.getData().isEmpty()) {
                log.warn("Account Master Customer MOU API returned success but no data for request list={}", customerMouBulkRequest);
                return Collections.emptyList();
            }

            log.info("Account Master Customer MOU POST API call successful. Records count={}",
                    responseBody.getData().size());

            return responseBody.getData();

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("HttpClientErrorException while calling Account Master Customer MOU POST API: {}",
                    ex.getMessage(), ex);
            throw new CentralCommerceServiceException(
                    String.format("HttpClientErrorException while calling Account Master Customer MOU POST API: %s",
                            ex.getMessage()), HttpStatus.valueOf(ex.getStatusCode().value()));
        } catch (Exception ex) {
            log.error("Exception occurred while calling while calling Account Master Customer MOU POST API: {}", ex.getMessage(), ex);
            throw new CentralCommerceServiceException(
                    String.format("Exception occurred while calling Account Master Customer MOU POST API: %s",
                            HttpStatus.INTERNAL_SERVER_ERROR)
            );
        }
    }
}
