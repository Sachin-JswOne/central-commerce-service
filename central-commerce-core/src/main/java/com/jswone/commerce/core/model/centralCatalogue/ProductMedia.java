package com.jswone.commerce.core.model.centralCatalogue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductMedia {
    private String public_url;
    private String content_type;
    private String file_name;
    private String asset_type;
    private MediaMetaData meta_data;
    private int rank;
}

