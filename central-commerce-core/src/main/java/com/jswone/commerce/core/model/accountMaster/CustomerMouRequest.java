package com.jswone.commerce.core.model.accountMaster;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerMouRequest {
    private String mouId;
    private String mouType;
}
