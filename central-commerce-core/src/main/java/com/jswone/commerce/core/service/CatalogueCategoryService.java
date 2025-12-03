package com.jswone.commerce.core.service;

import com.jswone.commerce.core.model.BreadcrumbData;
import com.jswone.commerce.core.model.CategoryTreeResponse;

public interface CatalogueCategoryService {
    CategoryTreeResponse getCatalogueCategoryTree();
    BreadcrumbData getBreadcrumbData(String categoryId);
}
