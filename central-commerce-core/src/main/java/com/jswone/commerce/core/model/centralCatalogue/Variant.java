package com.jswone.commerce.core.model.centralCatalogue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Variant {
    private long created_at;
    private Map<String, String> attributes;
    private String variant_mmid;
    private String id;
    private List<String> enabled_storefront;
    private int version;
}

