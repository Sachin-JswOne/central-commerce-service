package com.jswone.commerce.core.model.request;

import com.jswone.commerce.core.model.Attribute;
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
public class UomConvertRequest {

    @NotEmpty
    private String productMMID;
    @NotNull
    private Attribute customAttribute;
    @NotNull
    private Set<Attribute> productAttributes;
}
