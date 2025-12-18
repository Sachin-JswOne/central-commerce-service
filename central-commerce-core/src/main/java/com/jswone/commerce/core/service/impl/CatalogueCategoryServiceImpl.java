package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.mapper.BreadcrumbMapper;
import com.jswone.commerce.core.mapper.CategoryMapper;
import com.jswone.commerce.core.model.*;
import com.jswone.commerce.core.model.request.BulkCategoryRequestDTO;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.service.CatalogueCategoryService;
import java.util.List;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

  @Override
  public CategoryTreeResponse getBulkCatalogueCategoryTree(BulkCategoryRequestDTO categoryRequestDTO) {
    try {
      if(Objects.nonNull(categoryRequestDTO.getCategoryIds()) && Objects.nonNull(categoryRequestDTO.getBrandCategoryIds())){
        throw new CentralCommerceServiceException("Both category and brand cannot be called together", HttpStatus.BAD_REQUEST);
      }

      if(Objects.isNull(categoryRequestDTO.getBrandCategoryIds()) && Objects.isNull(categoryRequestDTO.getCategoryIds())){
        throw new CentralCommerceServiceException("Please provide either category or brand", HttpStatus.BAD_REQUEST);
      }

      CategoryTreeResponse categoryTreeResponse = this.getCatalogueCategoryTree();

      if(Objects.nonNull(categoryRequestDTO.getCategoryIds()) &&
              !categoryRequestDTO.getCategoryIds().isEmpty()){

        categoryTreeResponse.getNavigation().removeIf(navigationItem ->
                !navigationItem.getSlug().equalsIgnoreCase("jsw-main-menu-v2"));

        categoryTreeResponse.getNavigation().forEach(navigationItem ->
                navigationItem.getSubMenu().
                        removeIf(subMenu -> !categoryRequestDTO.getCategoryIds().contains(subMenu.getId())));

      } else if(Objects.nonNull(categoryRequestDTO.getBrandCategoryIds()) &&
              !categoryRequestDTO.getBrandCategoryIds().isEmpty()){

        categoryTreeResponse.getNavigation().removeIf(navigationItem ->
                !navigationItem.getSlug().equalsIgnoreCase("brands"));

        categoryTreeResponse.getNavigation().forEach(navigationItem ->
                navigationItem.getSubMenu().
                        removeIf(subMenu -> !categoryRequestDTO.getBrandCategoryIds().contains(subMenu.getId())));
      }
      return categoryTreeResponse;
    }catch (Exception ex){
      throw new CentralCommerceServiceException(ex.getMessage(),HttpStatus.BAD_REQUEST);
    }
  }
}
