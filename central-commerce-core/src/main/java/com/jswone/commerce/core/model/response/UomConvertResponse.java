package com.jswone.commerce.core.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jswone.commerce.core.model.Uom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UomConvertResponse {

    private String productMMID;
    private Uom primaryUom;
    private Uom secondaryUom;
}
