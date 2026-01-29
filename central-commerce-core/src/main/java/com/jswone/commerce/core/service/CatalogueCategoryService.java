package com.jswone.commerce.core.service;

import com.jswone.commerce.core.model.BreadcrumbData;
import com.jswone.commerce.core.model.CategoryTreeResponse;
import com.jswone.commerce.core.model.request.BulkCategoryRequestDTO;

public interface CatalogueCategoryService {
    CategoryTreeResponse getCatalogueCategoryTree();
    BreadcrumbData getBreadcrumbData(String categoryId, String slug);
    CategoryTreeResponse getBulkCatalogueCategoryTree(BulkCategoryRequestDTO categoryRequestDTO);
}
