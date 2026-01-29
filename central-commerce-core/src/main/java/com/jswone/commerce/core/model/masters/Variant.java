package com.jswone.commerce.core.model.masters;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Variant {
    @JsonProperty("hsn")
    private String hsn;

    @JsonProperty("tmt_form")
    private Object tmtForm;

    @JsonProperty("mmid")
    private String mmid;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("description")
    private String description;

    @JsonProperty("tax")
    private Integer tax;

    @JsonProperty("master_variant")
    private boolean masterVariant;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("variant_key")
    private int variantKey;

    @JsonProperty("variant_id")
    private int variantId;

    @JsonProperty("material_info")
    private String materialInfo;

    @JsonProperty("modified_by")
    private String modifiedBy;

    @JsonProperty("modified_at")
    private String modifiedAt;

    @JsonProperty("hash")
    private String hash;

    @JsonProperty("status")
    private String status;
}

