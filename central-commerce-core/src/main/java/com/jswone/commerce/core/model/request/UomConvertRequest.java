package com.jswone.commerce.core.model.request;

import com.jswone.commerce.core.model.PurchasedUom;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UomConvertRequest {

    @Valid
    @NotEmpty
    List<PurchasedUom> uomRequests;
}
