package com.jswone.commerce.core.entity.productCatalogueStore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collectionName = "product_catalogue_store")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductCatalogueStore {
    // Firestore document ID = Datastore Key Name
    @DocumentId
    private String identifier;
    private String status;
    private String productTitle;
    private String productSlug;
    private String productKey;
    private String metaTitle;
    private String metaDescription;
    private String productMaterialMasterId;
    private AttributeDisplay brand;
    private ProductMedia productMedia;
    private Image plpMedia;
    private Boolean hasVariant;
    private List<Variant> variants;
    private Journey distributedJourney;
    private Journey directJourney;
    private Journey pdpJourney;
    private double taxPercentage;
    private ProductOverview productOverview;
    private List<CompositionAttributes> compositionAttributes;
    private JSWPriceRange priceRange;
    private List<PLPAttribute> plpAttributes;
    private AttributeDisplay estimatedDelivery;
    private String defaultSelectedVariant;
    private Variant masterVariant;
    private BreadCrumbResource breadCrumbs;
    private Timestamp updatedAt;
    private CategoryAttributes mastersCategoryAttributes;
    private List<CartOrderDisplayCardAttributesDto> cartOrderDisplayCards;
    private List<String> categoryIds;
    private String brandValue;
    private String catalogueEnabledFor;
    private String grade;
    private boolean portalEnabled;
    private String productTypeKey;
}
