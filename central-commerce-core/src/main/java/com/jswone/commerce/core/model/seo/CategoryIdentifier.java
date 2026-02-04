package com.jswone.commerce.core.model.seo;

import com.jswone.commerce.core.enums.seo.CategoryType;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryIdentifier {
    String categoryId;
    String categorySlug;
    CategoryType categoryType;
}
