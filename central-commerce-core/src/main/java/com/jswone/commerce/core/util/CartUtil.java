package com.jswone.commerce.core.util;

import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commons.graphql.v2.CustomDataV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartUtil {

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
