package com.jswone.commerce.core.model.request.Search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jswone.commerce.core.model.response.plp.ProductFilterConditions;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Unified request model for Search API in Central Commerce Service (CCS). This merges existing CCP
 * Search API structure with Central Catalogue parameters.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    /** Text entered by user in search bar (mapped to Central Catalogue 'query') */
    @NotBlank(message = "Search text cannot be blank")
    private String text;

    /** Pagination offset (maps to Central Catalogue 'page') */
    @Builder.Default private Integer offSet = 0;

    /** Pagination limit (maps to Central Catalogue 'size') */
    @Builder.Default private Integer limit = 20;

    /** Current applied or available filters from the FE */
    private List<ProductFilterConditions> filterConditions;

    /**
     * New field — differentiates between typing vs explicit search. true → user clicked search
     * button false → user is still typing or paused
     */
    @Builder.Default private boolean searchAction = false;

    /** Optional storefront name (used when calling Central Catalogue) */
    @Builder.Default private String storefront = "default";

    @NotBlank(message = "Search text cannot be blank")
    private String searchId;

    @NotBlank(message = "Search text cannot be blank")
    private String searchType;
}
