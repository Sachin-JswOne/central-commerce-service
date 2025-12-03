package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.mapper.BreadcrumbMapper;
import com.jswone.commerce.core.mapper.CategoryMapper;
import com.jswone.commerce.core.model.*;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.service.CatalogueCategoryService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CatalogueCategoryServiceImpl implements CatalogueCategoryService {

  @Autowired private CentralCatalogueClient centralCatalogueClient;

  @Autowired private CategoryMapper categoryMapper;

  @Autowired private BreadcrumbMapper breadcrumbMapper;

  public CatalogueCategoryServiceImpl(
      CentralCatalogueClient centralCatalogueClient, CategoryMapper categoryMapper) {
    this.centralCatalogueClient = centralCatalogueClient;
    this.categoryMapper = categoryMapper;
  }

  @Override
  public CategoryTreeResponse getCatalogueCategoryTree() {

    List<CatalogueCategoryTree> catalogueCategoryTree = centralCatalogueClient.getCategoryTree();
    if (catalogueCategoryTree == null || catalogueCategoryTree.getFirst() == null) {
      log.error("Category tree API returned invalid or empty data");
      throw new CentralCommerceServiceException("Category tree API returned invalid or empty data");
    }

    log.info("Mapping catalogue data to central commerce format");
    List<NavigationItem> navigationList = categoryMapper.mapCategories(catalogueCategoryTree);
    CategoryTreeResponse categoryTreeResponse = new CategoryTreeResponse();
    categoryTreeResponse.setNavigation(navigationList);
    log.info("Category tree mapping completed successfully");
    return categoryTreeResponse;
  }

  @Override
  public BreadcrumbData getBreadcrumbData(String categoryId) {
    CatalogueBreadCrumbData catalogueBreadcrumbResponse =
        centralCatalogueClient.getBreadcrumb(categoryId);

    if (catalogueBreadcrumbResponse == null
        || catalogueBreadcrumbResponse.getBread_crumb_details() == null) {
      log.error(
          "Category breadcrumb API returned invalid or empty data for categoryId: {}", categoryId);
      throw new CentralCommerceServiceException(
          "Category breadcrumb API returned invalid or empty data for categoryId");
    }

    log.info("Mapping Catalogue breadcrumb data to central commerce format");
    return breadcrumbMapper.toBreadcrumbResponse(catalogueBreadcrumbResponse);
  }
}
