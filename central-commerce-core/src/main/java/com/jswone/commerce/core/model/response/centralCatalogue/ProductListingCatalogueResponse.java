package com.jswone.commerce.core.model.response.centralCatalogue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jswone.commerce.core.model.centralCatalogue.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductListingCatalogueResponse implements FacetsProvider {
    private List<Product> products;
    public Map<String, Set<String>> facets;

    @JsonProperty("total_hits")
    private long totalHits;

    @JsonProperty("current_page")
    private long currentPage;

    @JsonProperty("page_size")
    private long pageSize;
}
