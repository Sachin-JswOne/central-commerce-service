package com.jswone.commerce.core.model.seo;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryIdentifier {
    String categoryId;
    String categorySlug;
    String categoryType;
}
