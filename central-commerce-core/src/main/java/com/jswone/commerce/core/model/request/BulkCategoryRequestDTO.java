package com.jswone.commerce.core.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkCategoryRequestDTO {
    private List<String> categoryIds;
    private List<String> brandCategoryIds;
    private List<String> categorySlugs;
}
