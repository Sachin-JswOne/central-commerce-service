package com.jswone.commerce.core.model.response.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an individual search suggestion for dropdown display. Returned when searchAction =
 * false (user typing or paused).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSuggestion {

    /** Display text for suggestion (usually product title or keyword) */
    private String suggestionText;

    /** Slug for redirect to PDP when clicked */
    private String productSlug;

    /** Optional image to show in dropdown */
    private String imageUrl;

    /** Product MMID or key for internal reference */
    private String productMaterialMasterId;
}
