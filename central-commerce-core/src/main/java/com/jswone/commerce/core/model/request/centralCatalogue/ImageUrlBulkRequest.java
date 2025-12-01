package com.jswone.commerce.core.model.request.centralCatalogue;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUrlBulkRequest {
    private List<String> productMMIDS;
    private String storefront;
}
