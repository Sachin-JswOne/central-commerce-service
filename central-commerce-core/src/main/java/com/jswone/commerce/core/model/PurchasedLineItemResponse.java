package com.jswone.commerce.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jswone.commerce.core.entity.catalogue.QuantityCard;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

@Builder
@NoArgsConstructor
@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurchasedLineItemResponse {

    private String name;
    private String productKey;
    private String productSlug;
    private String variantKey;
    private String sku;
    private Map<String, String> attributes;
    private Map<String, String> ctAttributes;
    private Map<String, String> additionalAttributes;
    private QuantityCard quantityCard;
    private Set<String> attributesMeta;
    private Uom primaryUom;
    private Uom secondaryUom;
    private Uom ctUom;
    private String orderPlacedDate;
    private String productMMID;
    @JsonIgnore
    private String uniqueIdentifier;
    private String productTypeKey;
    private String variantMMID;
    private String imageUrl;
}
