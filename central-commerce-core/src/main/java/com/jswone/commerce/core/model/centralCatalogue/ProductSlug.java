package com.jswone.commerce.core.model.centralCatalogue;

import com.jswone.commerce.core.model.seo.SeoMeta;
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

    private Map<String, VariantSelector> variantSelectors;

    private List<Map<String, Object>> standardAttributes;

    private List<Map<String, Object>> customAttributes;

    private List<Map<String,Object>> productOverview;

    private boolean pdpIdentifier;

    private SeoMeta seoMeta;

    private  Map<String, Object> defaultSelectedAttributes;
}
