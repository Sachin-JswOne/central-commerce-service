package com.jswone.commerce.core.model.pricing;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurchasedQuantity {
    @NotNull(message = "cannot be null/blank")
    private String key;
    @NotNull(message = "cannot be null/blank")
    private String value;
}
