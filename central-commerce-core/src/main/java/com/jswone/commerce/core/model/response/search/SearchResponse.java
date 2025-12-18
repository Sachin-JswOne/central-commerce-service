package com.jswone.commerce.core.model.response.search;



import com.jswone.commerce.core.model.response.plp.PLPCard;
import com.jswone.commerce.core.model.response.plp.ProductFilterConditions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    /** Facet/filter conditions returned from Central Catalogue */
    private List<ProductFilterConditions> filterConditions;

    /** Final list of products (for PLP or search results) */
    private List<PLPCard> products;

    /** Dropdown suggestions (only populated when searchAction = false) */
    private List<SearchSuggestion> suggestions;

    /** Total product count for pagination and FE logic */
    private Long count;

    /** Total available results in catalogue */
    private Long total;

    /** Echoes back the original search query (for debugging/UI) */
    private String query;

    /** Indicates if this was triggered by user search action (true = clicked search CTA) */
    private boolean searchAction;
}
