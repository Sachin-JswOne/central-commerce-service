package com.jswone.commerce.core.model.response.plp;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PLPCard {
  private String productTitle;
  private String productSlug;
  private String imageUrl;
  private String altText;
  private String brand;
  private String deliveryInfo;
  private String distributedDeliveryInfo;
  private JSWPriceRange priceRange;
  private List<PLPAttribute> productAttributes;
  private String productMaterialMasterId;
}
