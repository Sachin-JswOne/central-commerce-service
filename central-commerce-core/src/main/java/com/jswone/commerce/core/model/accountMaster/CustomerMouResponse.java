package com.jswone.commerce.core.model.accountMaster;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerMouResponse {
    private Boolean success;
    private String message;
    private List<CustomerMouData> data;
}
