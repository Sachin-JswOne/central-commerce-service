package com.jswone.commerce.core.model.pricing;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jswone.commerce.core.model.Attribute;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Item {
    @NotNull(message = "cannot be null/blank")
    private PurchasedQuantity purchasedQuantity;
    @NotNull(message = "cannot be null/blank")
    private String productMMID;
    @NotNull(message = "cannot be null/blank")
    private List<Attribute> productAttributes;
}
