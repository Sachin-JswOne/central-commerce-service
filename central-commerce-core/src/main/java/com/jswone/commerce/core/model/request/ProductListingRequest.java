package com.jswone.commerce.core.model.request;

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
public class ProductListingRequest implements FilterRequestProvider {

    /** Pagination offset (maps to Central Catalogue 'page') */
    @Builder.Default private Integer offSet = 0;

    /** Pagination limit (maps to Central Catalogue 'size') */
    @Builder.Default private Integer limit = 21;

    /** Optional storefront name (used when calling Central Catalogue) */
    @Builder.Default private String storefront = "msme";

    private String categoryId;

    private String slug;

    /** Current applied or available filters from the FE */
    private List<ProductFilterConditions> filterConditions;

}
