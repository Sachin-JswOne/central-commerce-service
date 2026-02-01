package com.jswone.commerce.core.model.mou;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MouGstEntity {
    private String entityName;
    private String gstin;
//    private String relationship;
}
