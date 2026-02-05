package com.jswone.commerce.core.mapper;

import com.jswone.commerce.core.constants.GenericConstants;
import com.jswone.commerce.core.model.centralCatalogue.Product;
import com.jswone.commerce.core.model.centralCatalogue.ProductSlug;
import com.jswone.commerce.core.model.centralCatalogue.QuantityCard;
import com.jswone.commerce.core.model.centralCatalogue.VariantSelector;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductSlugMapper {

    ProductSlug toProductSlug(Product product, List<QuantityCard> quantityCards);

    default Map<String, VariantSelector> updateVariantSelectors(Map<String, VariantSelector> existingSelectors,
                                                                Map<String, Object> attributes, Map<String, String> variantNames) {
        if (Objects.isNull(existingSelectors) || existingSelectors.isEmpty()) {
            return existingSelectors;
        }
        existingSelectors.forEach((key, variantSelector) -> {
            variantSelector.setDisplayName(variantNames.get(key));
            variantSelector.setMin(Optional.ofNullable(attributes.get(key + GenericConstants.CENTRAL_CATALOGUE_MIN_SUFFIX)).filter(Number.class::isInstance)
                    .map(Double.class::cast).orElse(null));
            variantSelector.setMax(Optional.ofNullable(attributes.get(key + GenericConstants.CENTRAL_CATALOGUE_MAX_SUFFIX)).filter(Number.class::isInstance)
                    .map(Double.class::cast).orElse(null));
        });
        return existingSelectors;
    }

    default List<Map<String, Object>> updateCustomAttributes(List<Map<String, Object>> customAttributes, Map<String, Object> values) {
        if (Objects.isNull(customAttributes) || customAttributes.isEmpty()) {
            return customAttributes;
        }
        customAttributes.forEach(customAttributeMap -> {
            String attributeKey = (String) customAttributeMap.get(GenericConstants.CENTRAL_CATALOGUE_CUSTOM_ATTRIBUTE_KEY);
            if (values.containsKey(attributeKey)) {
                customAttributeMap.put(GenericConstants.CENTRAL_CATALOGUE_CUSTOM_ATTRIBUTE_VALUE, values.get(attributeKey));
            }
        });
        return customAttributes;
    }
}
