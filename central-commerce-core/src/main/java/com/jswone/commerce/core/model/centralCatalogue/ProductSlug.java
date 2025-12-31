package com.jswone.commerce.core.model.centralCatalogue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSlug {
    private String id;

    private int version;

    private Map<String, Object> attributes;

    private List<Variant> variants;

    private String productTypeId;

    private String productMmid;

    private List<AssociatedCategory> associatedCategories;

    private String createdAt;

    private String lastModifiedAt;

    private List<String> enabledStorefronts;

    private MetaData metaData;

    private Double rank;

    private List<ProductLocation> productLocation;

    private List<QuantityCard> quantityCards;
}
