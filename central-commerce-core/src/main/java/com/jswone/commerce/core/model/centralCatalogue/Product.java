package com.jswone.commerce.core.model.centralCatalogue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private String id;
    private int version;
    private ProductAttributes attributes;
    private List<Variant> variants;
    private String product_type_id;
    private String product_mmid;
    private List<AssociatedCategory> associated_categories;
    private String created_at;
    private String last_modified_at;
    private List<String> enabled_storefronts;
    private MetaData meta_data;
    private Double rank;
    private List<ProductLocation> product_location;
}

