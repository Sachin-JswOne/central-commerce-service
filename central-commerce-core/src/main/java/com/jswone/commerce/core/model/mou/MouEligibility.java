package com.jswone.commerce.core.model.mou;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MouEligibility {
    private String gstin;
    private String financialYear;
    private List<CustomerMouDetails> customerMouDetails;
}
