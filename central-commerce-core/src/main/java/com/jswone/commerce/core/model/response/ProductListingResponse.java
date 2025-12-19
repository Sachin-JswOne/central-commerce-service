package com.jswone.commerce.core.model.response;

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
public class ProductListingResponse {

    /** Facet/filter conditions returned from Central Catalogue */
    private List<ProductFilterConditions> filterConditions;

    /** Final list of products (for PLP or search results) */
    private List<PLPCard> products;

    /** Total product count for pagination and FE logic */
    private Long count;

    /** Total available results in catalogue */
    private Long total;

    /** Indicates if this was triggered by user search action (true = clicked search CTA) */
    private String categoryId;
}
