package com.jswone.commerce.core.model.request.centralCatalogue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSlugRequestDTO {
    private String slug;
    private String storefront;
    private String locale;
    private Map<String, List<String>> filters;

}
