package com.jswone.commerce.core.entity.catalogue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Key;
import com.google.cloud.spring.data.datastore.core.mapping.Entity;
import com.google.cloud.spring.data.datastore.core.mapping.Unindexed;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "product_catalogue_store")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductCatalogueStore {
  @Id
  @JsonProperty("identifier")
  Key identifier;

  private String status;
  @Unindexed private String productTitle;
  private String productSlug;
  private String productKey;
  private String metaTitle;
  private String metaDescription;
  private String productMaterialMasterId;
  @Unindexed private AttributeDisplay brand;
  @Unindexed private ProductMedia productMedia;
  @Unindexed private Image plpMedia;
  @Unindexed private Boolean hasVariant;
  @Unindexed private List<Variant> variants;
  @Unindexed private Journey distributedJourney;
  @Unindexed private Journey directJourney;
  @Unindexed private Journey pdpJourney;
  @Unindexed private double taxPercentage;
  @Unindexed private ProductOverview productOverview;
  @Unindexed private List<CompositionAttributes> compositionAttributes;
  @Unindexed private JSWPriceRange priceRange;
  @Unindexed private List<PLPAttribute> plpAttributes;
  @Unindexed private AttributeDisplay estimatedDelivery;
  @Unindexed private String defaultSelectedVariant;
  @Unindexed private Variant masterVariant;
  @Unindexed private BreadCrumbResource breadCrumbs;
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
