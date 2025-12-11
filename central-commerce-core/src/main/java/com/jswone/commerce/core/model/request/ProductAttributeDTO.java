package com.jswone.commerce.core.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributeDTO {
    @NotNull(message = "cannot be null/blank")
    private String key;
    @NotNull(message = "cannot be null/blank")
    private Object value;
    private String unit;
}
