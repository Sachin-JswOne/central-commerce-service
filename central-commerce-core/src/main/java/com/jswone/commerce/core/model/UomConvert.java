package com.jswone.commerce.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UomConvert {
    @NotNull(message = "cannot be null/blank")
    private String requestIdentifier;
    private String productMMID;
    private UomValueDetails<?> primaryUom;
    private UomValueDetails<?> purchasedUom;
    private boolean primaryUomPurchased;
    private boolean success;
    private String errorMessage;
}
