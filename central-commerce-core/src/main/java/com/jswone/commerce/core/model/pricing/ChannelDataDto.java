package com.jswone.commerce.core.model.pricing;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChannelDataDto {
    private LineItemPriceDto quantity;
    private LineItemPriceDto availableQuantity;
    private String channelId;
    private String channelKey;
    private String channelName;
    private double availableQuantityInCTUOM;
    private String sellerName;
}
