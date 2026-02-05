package com.jswone.commerce.core.model.accountMaster;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerMouData {

    private Long id;
    private String mouId;
    private String pan;
    private String gstin;
    private String entityName;
    private String entityRelationship;
    private boolean mouDisplay;
    private String financialYear;
    private String mouType;
}
