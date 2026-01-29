package com.jswone.commerce.core.service;

import com.jswone.commerce.core.model.BreadcrumbData;
import com.jswone.commerce.core.model.CategoryTreeResponse;
import com.jswone.commerce.core.model.request.BulkCategoryRequestDTO;

public interface CatalogueCategoryService {
    CategoryTreeResponse getCatalogueCategoryTree();
    CategoryTreeResponse getBulkCatalogueCategoryTree(BulkCategoryRequestDTO categoryRequestDTO);
}
