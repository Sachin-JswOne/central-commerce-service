package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.mapper.CategoryMapper;
import com.jswone.commerce.core.model.CatalogueCategoryTreeResponse;
import com.jswone.commerce.core.model.CategoryTreeResponse;
import com.jswone.commerce.core.model.NavigationItem;
import com.jswone.commerce.core.service.CatalogueCategoryService;
import com.jswone.commerce.core.service.CentralCatalogueClient;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CatalogueCategoryServiceImpl implements CatalogueCategoryService {

  @Autowired private CentralCatalogueClient centralCatalogueClient;

  @Autowired private CategoryMapper categoryMapper;

  public CatalogueCategoryServiceImpl(
      CentralCatalogueClient centralCatalogueClient, CategoryMapper categoryMapper) {
    this.centralCatalogueClient = centralCatalogueClient;
    this.categoryMapper = categoryMapper;
  }

  public CategoryTreeResponse getCatalogueCategoryTree() {

    CatalogueCategoryTreeResponse catalogueCategoryTree =
        centralCatalogueClient.getCatalogueCategoryTree();
    if (catalogueCategoryTree == null || catalogueCategoryTree.getData() == null) {
      log.error("Catalogue category tree data not found");
      throw new CentralCommerceServiceException(
          "Catalogue category tree data not found", HttpStatus.NOT_FOUND);
    }

    log.info("Mapping catalogue data to central commerce format");
    List<NavigationItem> navigationList =
        categoryMapper.mapCategories(catalogueCategoryTree.getData());
    CategoryTreeResponse categoryTreeResponse = new CategoryTreeResponse();
    categoryTreeResponse.setNavigation(navigationList);
    log.info("Category tree mapping completed successfully");
    return categoryTreeResponse;
  }
}
