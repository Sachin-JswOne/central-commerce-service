package com.jswone.commerce.core.model.request.centralCatalogue;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class CentralCatalogueProductListingRequest {
    private Integer page;
    private Integer size;
    private String category_id;
    private String storefront;
    private String locale;
    private Boolean facets_only;
    private Map<String, List<String>> filters;
}
