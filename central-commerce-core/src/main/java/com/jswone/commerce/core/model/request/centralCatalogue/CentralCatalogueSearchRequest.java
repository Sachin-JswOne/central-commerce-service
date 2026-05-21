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
public class CentralCatalogueSearchRequest {
    private String query;
    private Integer page;
    private Integer size;
    private String storefront;
    private String locale;
    private Boolean facets_only;
    private Map<String, List<String>> filters;
    private Boolean sortOnRank;
}
