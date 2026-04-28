package com.jswone.commerce.core.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AiInvokeRequest {

    @NotBlank(message = "PAN cannot be null/blank")
    private String pan;

    @NotBlank(message = "GSTIN cannot be null/blank")
    private String gstin;

    @NotBlank(message = "Company name cannot be null/blank")
    private String companyName;

    @NotBlank(message = "Location cannot be null/blank")
    private String location;

    private String businessDescription;
}
