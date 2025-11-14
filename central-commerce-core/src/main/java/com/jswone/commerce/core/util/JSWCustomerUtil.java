package com.jswone.commerce.core.util;

import com.commercetools.api.client.ApiRoot;
import com.commercetools.api.client.ByProjectKeyRequestBuilder;
import com.commercetools.api.models.common.Address;
import com.commercetools.api.models.customer.Customer;
import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.config.JSWCommerceToolsConfig;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.service.ClientService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static com.jswone.commerce.core.constants.JSWChannelConstants.COMMA_SPACE_SEPARATOR;

@Component
@Log4j2
public class JSWCustomerUtil {


    private final ClientService clientService;

    private final CommerceValueConfig commerceValueConfig;

    @Autowired
    @Qualifier("requestBuilderCTAdmin") ByProjectKeyRequestBuilder requestBuilderCT;

    public JSWCustomerUtil(ClientService clientService, CommerceValueConfig commerceValueConfig) {
        this.clientService = clientService;
        this.commerceValueConfig = commerceValueConfig;
    }

    public Customer getCurrentCustomer(String accessToken) {
        ApiRoot myCustomerRoot = null;
        Customer customerResponse = null;
        try {
            myCustomerRoot =
                    clientService.createConstantTokenApiClient(accessToken, commerceValueConfig.getApiUrl());
            customerResponse =
                    myCustomerRoot
                            .withProjectKey(commerceValueConfig.getProjectKey())
                            .me()
                            .get()
                            .executeBlocking()
                            .getBody();
        } catch (IOException e) {
            return null;
        } finally {
            if (myCustomerRoot != null) {
                myCustomerRoot.close();
            }
        }
        return customerResponse;
    }

    public Customer getCustomerById(String customerId) {
        Customer customerResponse = null;
        try {
            customerResponse =
                    requestBuilderCT
                            .customers()
                            .withId(customerId)
                            .get()
                            .executeBlocking()
                            .getBody();
        } catch (Exception e) {
            throw new CentralCommerceServiceException(e.getMessage());
        }
        return customerResponse;
    }

    public String getdefaultShipAddressLocation(Customer c) {
        Address addressObj = getDefaultShippingAddress(c);
        Optional<Address> address =
                Objects.nonNull(addressObj) ? Optional.of(addressObj) : Optional.empty();
        return address.map(Address::getPostalCode).orElse(null);
    }

    public String getSecondaryLocation(Customer c) {
        Address address = getDefaultShippingAddress(c);
        return getAddressForDistanceCalculation(address);
    }

    public String getAddressForDistanceCalculation(Address address) {
        StringBuilder mapAddress = new StringBuilder();
        if (address == null) return mapAddress.toString();
        if (StringUtils.isNotBlank(address.getPostalCode())
                && StringUtils.isNotBlank(address.getState())) {
            mapAddress
                    .append(address.getPostalCode())
                    .append(COMMA_SPACE_SEPARATOR)
                    .append(address.getState());
            return mapAddress.toString();
        } else if (StringUtils.isNotBlank(address.getPostalCode())) {
            return address.getPostalCode();
        } else if (StringUtils.isNotBlank(address.getState())) {
            return address.getState();
        } else {
            return mapAddress.toString();
        }
    }

    private Address getDefaultShippingAddress(Customer customer) {
        return customer.getAddresses().stream()
                .filter(Objects::nonNull)
                .filter(
                        address ->
                                address.getId()
                                        .equalsIgnoreCase(customer.getDefaultShippingAddressId()))
                .findFirst()
                .orElse(null);
    }

    public String getCustomerId() {
        try {
            UserDetails userDetails =
                    (UserDetails)
                            SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userDetails.getUsername().contains("Customer")) {
                throw new CentralCommerceServiceException(
                        "JWT validation failed : Please provide access token instead of X-API-KEY");
            }
            return userDetails.getUsername();
        } catch (Exception e) {
            throw new CentralCommerceServiceException(e.getLocalizedMessage());
        }
    }
}

