package com.jswone.commerce.core.model.request.centralCatalogue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSlugRequestDTO {
    private String slug;
    private String storefront;
    private String locale;

}
