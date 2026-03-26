package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.mapper.BreadcrumbMapper;
import com.jswone.commerce.core.mapper.CategoryMapper;
import com.jswone.commerce.core.model.*;
import com.jswone.commerce.core.model.request.BulkCategoryRequestDTO;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.service.CatalogueCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Stream;

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
  public BreadcrumbData getBreadcrumbData(String categoryId, String slug) {
    try {
      if(Objects.nonNull(categoryId) && Objects.nonNull(slug)){
        throw new CentralCommerceServiceException("Both category and slug cannot be called together", HttpStatus.BAD_REQUEST);
      }

      if(Objects.isNull(categoryId) && Objects.isNull(slug)){
        throw new CentralCommerceServiceException("Please provide either category or slug", HttpStatus.BAD_REQUEST);
      }
      CatalogueBreadCrumbData catalogueBreadcrumbResponse =
              centralCatalogueClient.getBreadcrumb(categoryId, slug);

      if (catalogueBreadcrumbResponse == null
              || catalogueBreadcrumbResponse.getBread_crumb_details() == null) {
        log.error(
                "Category breadcrumb API returned invalid or empty data for categoryId: {}", categoryId);
        throw new CentralCommerceServiceException(
                "Category breadcrumb API returned invalid or empty data for categoryId");
      }

      log.info("Mapping Catalogue breadcrumb data to central commerce format");
      return breadcrumbMapper.toBreadcrumbResponse(catalogueBreadcrumbResponse);
    }catch (Exception ex){
      throw new CentralCommerceServiceException(ex.getMessage(),HttpStatus.BAD_REQUEST);
    }
  }

    @Override
    public CategoryTreeResponse getBulkCatalogueCategoryTree(BulkCategoryRequestDTO categoryRequestDTO) {

        try {
            if (Objects.nonNull(categoryRequestDTO.getCategoryIds())
                    && Objects.nonNull(categoryRequestDTO.getBrandCategoryIds())) {
                throw new CentralCommerceServiceException(
                        "Both category and brand cannot be called together",
                        HttpStatus.BAD_REQUEST);
            }

            if (Objects.isNull(categoryRequestDTO.getBrandCategoryIds())
                    && Objects.isNull(categoryRequestDTO.getCategoryIds())
                    && Objects.isNull(categoryRequestDTO.getCategorySlugs())) {
                throw new CentralCommerceServiceException(
                        "Please provide either category or brand",
                        HttpStatus.BAD_REQUEST);
            }

            if ((Objects.nonNull(categoryRequestDTO.getBrandCategoryIds())
                    && categoryRequestDTO.getBrandCategoryIds().isEmpty())
                    || (Objects.nonNull(categoryRequestDTO.getCategoryIds())
                    && categoryRequestDTO.getCategoryIds().isEmpty())) {
                throw new CentralCommerceServiceException(
                        "Please provide either category or brand",
                        HttpStatus.BAD_REQUEST);
            }

            CategoryTreeResponse categoryTreeResponse =
                    this.getCatalogueCategoryTree();

            //Category filtering logic
            if (Objects.nonNull(categoryRequestDTO.getCategoryIds()) &&
                    !categoryRequestDTO.getCategoryIds().isEmpty()) {

                getCategoryTreeResponse(categoryTreeResponse, categoryRequestDTO);
            }

            // Brand filtering logic
            else if (Objects.nonNull(categoryRequestDTO.getBrandCategoryIds()) &&
                    !categoryRequestDTO.getBrandCategoryIds().isEmpty()) {

                getBrandCategoryTreeResponse(categoryTreeResponse,categoryRequestDTO);
            }

            //Slug Category filtering logic
            else if (Objects.nonNull(categoryRequestDTO.getCategorySlugs()) &&
                    !categoryRequestDTO.getCategorySlugs().isEmpty())
            {
                getSlugCategoryTreeResponse(categoryTreeResponse,categoryRequestDTO);
            }
            return categoryTreeResponse;

        } catch (Exception ex) {
            throw new CentralCommerceServiceException(
                    ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public CategoryTreeResponse getSearchedCatalogueCategoryTree(Set<String> categoryIds) {
        SearchedCategoryTree catalogueCategoryTree = centralCatalogueClient.getSearchedCategoryTree(categoryIds);
        if (catalogueCategoryTree == null) {
            log.error("Category tree getSearchedCatalogueCategoryTree returned invalid or empty data");
            throw new CentralCommerceServiceException("Category tree getSearchedCatalogueCategoryTree returned invalid or empty data");
        }
        List<CatalogueCategoryTree> catalogueCategoryTreeResponse =
                Stream.of(Optional.ofNullable(catalogueCategoryTree.getAllProducts())
                                        .orElse(Collections.emptyList()),
                                Optional.ofNullable(catalogueCategoryTree.getIndustrySegments())
                                        .orElse(Collections.emptyList()))
                        .flatMap(Collection::stream)
                        .toList();
        log.info("Mapping getSearchedCatalogueCategoryTree to central commerce format");
        List<NavigationItem> navigationList = categoryMapper.mapCategories(catalogueCategoryTreeResponse);
        CategoryTreeResponse categoryTreeResponse = new CategoryTreeResponse();
        categoryTreeResponse.setNavigation(navigationList);
        log.info("getSearchedCatalogueCategoryTree mapping completed successfully");
        return categoryTreeResponse;
    }

    private void getCategoryTreeResponse(CategoryTreeResponse categoryTreeResponse, BulkCategoryRequestDTO categoryRequestDTO){
        List<String> categoryIds = categoryRequestDTO.getCategoryIds()
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        categoryTreeResponse.getNavigation().removeIf(
                nav -> !"All products".equalsIgnoreCase(nav.getName()));

        categoryTreeResponse.getNavigation().forEach(nav -> {

            List<NavigationItem> matchedSubMenus = new ArrayList<>();

            for (String categoryId : categoryIds) {
                NavigationItem matched =
                        findNodeRecursively(nav.getSubMenu(), categoryId, "category");
                if (matched != null) {
                    matchedSubMenus.add(matched);
                }
            }
            nav.setSubMenu(matchedSubMenus);
        });
    }

    private void getBrandCategoryTreeResponse(CategoryTreeResponse categoryTreeResponse, BulkCategoryRequestDTO categoryRequestDTO){
        List<String> brandCategoryIds =
                categoryRequestDTO.getBrandCategoryIds()
                        .stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

        categoryTreeResponse.getNavigation().removeIf(
                nav -> !"Brands".equalsIgnoreCase(nav.getName()));

        categoryTreeResponse.getNavigation().forEach(nav -> {

            List<NavigationItem> matchedSubMenus = new ArrayList<>();

            for (String brandId : brandCategoryIds) {
                NavigationItem matched =
                        findNodeRecursively(nav.getSubMenu(), brandId, "brand");
                if (matched != null) {
                    matchedSubMenus.add(matched);
                }
            }
            nav.setSubMenu(matchedSubMenus);
        });
    }

    private void getSlugCategoryTreeResponse(CategoryTreeResponse categoryTreeResponse, BulkCategoryRequestDTO categoryRequestDTO){
        List<String> slugs = categoryRequestDTO.getCategorySlugs()
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        categoryTreeResponse.getNavigation().removeIf(
                nav -> !"All products".equalsIgnoreCase(nav.getName()));

        if(!slugs.isEmpty() && !slugs.contains("all-products")) {
            categoryTreeResponse.getNavigation().forEach(nav -> {

                List<NavigationItem> matchedSubMenus = new ArrayList<>();

                for (String slug : slugs) {
                    NavigationItem matched =
                            findNodeRecursively(nav.getSubMenu(), slug, "slug");
                    if (matched != null) {
                        matchedSubMenus.add(matched);
                    }
                }
                nav.setSubMenu(matchedSubMenus);
            });
        }
    }

    private NavigationItem findNodeRecursively(
            List<NavigationItem> subMenus, String targetId, String findBy) {

        if (subMenus == null || targetId == null) {
            return null;
        }

        for (NavigationItem item : subMenus) {
            if (matches(item, targetId, findBy)) {
                return item;
            }
            NavigationItem found = findNodeRecursively(item.getSubMenu(), targetId, findBy);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private boolean matches(NavigationItem item, String targetId, String findBy) {

        return switch (findBy) {
            case "category", "brand" -> targetId.equals(item.getId());
            case "slug" -> targetId.equals(item.getSlug());
            default -> false;
        };
    }
}
