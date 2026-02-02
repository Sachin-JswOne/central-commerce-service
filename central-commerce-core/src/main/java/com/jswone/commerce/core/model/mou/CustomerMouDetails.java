package com.jswone.commerce.core.model.mou;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerMouDetails {
    private String mouId;
    private String mouType;
    private boolean mouDisplay;
    private List<MouGstEntity> entities;
}
