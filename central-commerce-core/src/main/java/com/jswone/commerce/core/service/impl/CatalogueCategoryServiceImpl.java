package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.enums.seo.SeoEntityType;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.mapper.BreadcrumbMapper;
import com.jswone.commerce.core.mapper.CategoryMapper;
import com.jswone.commerce.core.model.*;
import com.jswone.commerce.core.model.request.BulkCategoryRequestDTO;
import com.jswone.commerce.core.model.seo.SeoContext;
import com.jswone.commerce.core.model.seo.SeoData;
import com.jswone.commerce.core.resolver.SeoContextResolver;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.service.CatalogueCategoryService;
import com.jswone.commerce.core.service.SeoService;
import com.jswone.commerce.core.util.CatalogueUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@Service
@Slf4j
public class CatalogueCategoryServiceImpl implements CatalogueCategoryService {

    @Autowired
    private CentralCatalogueClient centralCatalogueClient;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private BreadcrumbMapper breadcrumbMapper;

    private final SeoContextResolver seoContextResolver;

    private final SeoService seoService;

    public CatalogueCategoryServiceImpl(
            CentralCatalogueClient centralCatalogueClient, CategoryMapper categoryMapper,
            SeoContextResolver seoContextResolver, SeoService seoService) {
        this.centralCatalogueClient = centralCatalogueClient;
        this.categoryMapper = categoryMapper;
        this.seoContextResolver = seoContextResolver;
        this.seoService = seoService;
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

            boolean hasCategoryId = StringUtils.isNotBlank(categoryId);
            boolean hasSlug = StringUtils.isNotBlank(slug);

            if (hasCategoryId && hasSlug) {
                throw new CentralCommerceServiceException(
                        "Provide either categoryId or slug, not both",
                        HttpStatus.BAD_REQUEST);
            }

            if (!hasCategoryId && !hasSlug) {
                throw new CentralCommerceServiceException(
                        "Either categoryId or slug must be provided",
                        HttpStatus.BAD_REQUEST);
            }

            SeoContext seoContext = null;
            String extractedSlug = null;

            if (hasSlug) {
                // Use entity type hint - no manual prefixing needed
                seoContext = seoContextResolver.resolve(slug, SeoEntityType.CATEGORY);
                extractedSlug = seoContext.getSlug();
            }

            CatalogueBreadCrumbData response = centralCatalogueClient.getBreadcrumb(
                    hasCategoryId ? categoryId : null,
                    extractedSlug);

            validateBreadcrumbResponse(response, categoryId);

            if (hasSlug) {
                enrichSeo(response, seoContext);
            }

            return breadcrumbMapper.toBreadcrumbResponse(response);

        } catch (Exception e) {
            throw new CentralCommerceServiceException(
                    e.getLocalizedMessage(),
                    HttpStatus.BAD_GATEWAY);
        }
    }

    private void validateBreadcrumbResponse(
            CatalogueBreadCrumbData response,
            String categoryId) {

        if (response == null || response.getBread_crumb_details() == null) {

            log.error("Breadcrumb API returned invalid data for categoryId: {}", categoryId);

            throw new CentralCommerceServiceException(
                    "Category breadcrumb API returned invalid or empty data",
                    HttpStatus.BAD_GATEWAY);
        }
    }


    private void enrichSeo(
            CatalogueBreadCrumbData response,
            SeoContext seoContext) {

        response.getBread_crumb_details().forEach(detail -> {

            String title = detail.getAttributes().getCategory_title();

            if (seoContext.getLocation() != null) {
                title = title + " in " +
                        CatalogueUtil.formatSeoLocationToTitleCase(seoContext.getLocation());
                detail.getAttributes().setCategory_title(title);
            }

            SeoData seoData = SeoData.builder()
                    .title(title)
                    .image(EMPTY)
                    .build();

            detail.getAttributes()
                    .setSeo_meta(seoService.resolveSeoMeta(seoContext, seoData));
        });
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
