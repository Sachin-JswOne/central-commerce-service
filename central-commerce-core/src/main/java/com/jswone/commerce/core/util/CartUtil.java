package com.jswone.commerce.core.util;

import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commons.graphql.v2.CustomDataV2;
import com.jswone.commons.graphql.v2.OrdersV2;
import com.jswone.commons.graphql.v2.ResultV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartUtil {

    public static Set<String> getProductMMID(OrdersV2 cart, String value){
        return Optional.ofNullable(cart)
                .map(OrdersV2::getResults)
                .stream()
                .flatMap(Collection::stream)
                .findFirst()
                .map(ResultV2::getLineItems)
                .map(lineItems ->
                        lineItems.stream()
                                .map(li -> CartUtil.customDataV2toString(li.getCustom().getCustomFieldsRaw(),value))
                                .collect(Collectors.toSet()))
                .orElse(null);
    }

    public static String customDataV2toString(List<CustomDataV2> customData,String value){
        CustomDataV2 customDataV2 = customData.stream()
                .filter(custom -> value.equalsIgnoreCase(custom.getName()))
                .findFirst()
                .orElseThrow(
                        () ->
                                new CentralCommerceServiceException(
                                        String.format("%s missing in custom field",value),
                                        HttpStatus.NO_CONTENT));
        return String.valueOf(customDataV2.getValue());
    }
}
