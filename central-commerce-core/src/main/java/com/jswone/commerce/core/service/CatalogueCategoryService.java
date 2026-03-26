package com.jswone.commerce.core.service;

import com.jswone.commerce.core.model.BreadcrumbData;
import com.jswone.commerce.core.model.CategoryTreeResponse;
import com.jswone.commerce.core.model.request.BulkCategoryRequestDTO;

import java.util.Set;

public interface CatalogueCategoryService {
    CategoryTreeResponse getCatalogueCategoryTree();
    BreadcrumbData getBreadcrumbData(String categoryId, String slug);
    CategoryTreeResponse getBulkCatalogueCategoryTree(BulkCategoryRequestDTO categoryRequestDTO);
    CategoryTreeResponse getSearchedCatalogueCategoryTree(Set<String> categoryIds);
}
