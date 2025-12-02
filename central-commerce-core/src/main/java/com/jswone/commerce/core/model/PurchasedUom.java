package com.jswone.commerce.core.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PurchasedUom {

    @NotEmpty
    private String productMMID;
    private String variantMMID;
    @NotNull
    private Attribute purchasedUom;
    @NotNull
    private Set<Attribute> productAttributes;
}
