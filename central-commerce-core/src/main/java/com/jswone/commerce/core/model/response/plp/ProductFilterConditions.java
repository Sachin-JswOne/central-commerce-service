package com.jswone.commerce.core.model.response.plp;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProductFilterConditions {
    private String id;
    private String displayText;
    private String type;
    private List<String> values;
    private List<String> selectedValues;
}
