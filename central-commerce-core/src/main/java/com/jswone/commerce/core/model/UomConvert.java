package com.jswone.commerce.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    private String productMMID;
    private UomValueDetails<?> primaryUom;
    private UomValueDetails<?> secondaryUom;
    private boolean success;
    private String errorMessage;
}
