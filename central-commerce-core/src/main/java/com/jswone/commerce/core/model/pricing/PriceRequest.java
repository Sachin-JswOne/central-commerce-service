package com.jswone.commerce.core.model.pricing;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PriceRequest {
    @NotNull(message = "cannot be null/blank")
    private String pinCode;
    @NotNull(message = "cannot be null/blank")
    private Set<Item> items;
}
