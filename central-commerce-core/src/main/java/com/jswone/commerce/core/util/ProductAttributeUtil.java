package com.jswone.commerce.core.util;

import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.constants.JSWProductConstants;
import com.jswone.commerce.core.entity.catalogue.Attribute;
import com.jswone.commerce.core.enums.MaterialMasterToCTAttribute;
import com.jswone.commerce.core.model.request.ProductAttributeDTO;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Log4j2
@Component
public class ProductAttributeUtil {

    private final CommerceValueConfig commerceValueConfig;

    public ProductAttributeUtil(CommerceValueConfig commerceValueConfig) {
        this.commerceValueConfig = commerceValueConfig;
    }



    public List<ProductAttributeDTO> convertAttributes(List<ProductAttributeDTO> inputAttributes) {

        List<ProductAttributeDTO> output = new ArrayList<>();

        for (ProductAttributeDTO attr : inputAttributes) {
            String ctKey = attr.getKey().toUpperCase();

            try {
                String enumEntry =
                        MaterialMasterToCTAttribute.valueOf(ctKey.toUpperCase()).getMaterialMasterCTAttribute();
                output.add(new ProductAttributeDTO(enumEntry, attr.getValue(),attr.getUnit()));

            } catch (IllegalArgumentException e) {
                output.add(attr);
            }
        }

        return output;
    }

    public Attribute transformToAttribute(ProductAttributeDTO request) {
        String[] productSkuFilters =
                commerceValueConfig.getProductSkuDoubleAttributes().split(",");
        return Attribute.builder()
                .name(
                        request.getKey()
                                .concat(
                                        request.getUnit() != null
                                                ? JSWProductConstants.UNDERSCORE
                                                : JSWProductConstants.EMPTY_STRING)
                                .concat(
                                        request.getUnit() != null
                                                ? request.getUnit()
                                                : JSWProductConstants.EMPTY_STRING))
                .value(
                        Arrays.stream(productSkuFilters).anyMatch(request.getKey()::contains)
                                ? castToDouble(request)
                                : request.getValue())
                .build();
    }

    public static Double castToDouble(ProductAttributeDTO request) {
        return Double.parseDouble(request.getValue().toString());
    }
}
