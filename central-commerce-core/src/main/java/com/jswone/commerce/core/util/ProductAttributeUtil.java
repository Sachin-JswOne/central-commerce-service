package com.jswone.commerce.core.util;

import com.commercetools.api.models.product.AttributeBuilder;
import com.jswone.commerce.core.constants.JSWProductConstants;
import com.jswone.commerce.core.entity.Attribute;
import com.jswone.commerce.core.enums.MaterialMasterToCTAttribute;
import com.jswone.commerce.core.model.request.ProductAttributeDTO;
import com.jswone.commons.util.PropertyLoader;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Log4j2
@Component
public class ProductAttributeUtil {
    private static final PropertyLoader propertyLoader = PropertyLoader.getInstance();
    static final String[] productSkuFilters =
            propertyLoader.getProperty("product.sku.double.attributes").split(",");

    public List<ProductAttributeDTO> convertAttributes(List<ProductAttributeDTO> inputAttributes) {

        List<ProductAttributeDTO> output = new ArrayList<>();

        for (ProductAttributeDTO attr : inputAttributes) {
            String ctKey = attr.getKey().toUpperCase();

            try {
                MaterialMasterToCTAttribute enumEntry =
                        MaterialMasterToCTAttribute.valueOf(ctKey);
                output.add(new ProductAttributeDTO(enumEntry.toString(), attr.getValue(),attr.getUnit()));

            } catch (IllegalArgumentException e) {
                output.add(attr);
            }
        }

        return output;
    }

    public static Attribute transformToAttribute(ProductAttributeDTO request) {
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
