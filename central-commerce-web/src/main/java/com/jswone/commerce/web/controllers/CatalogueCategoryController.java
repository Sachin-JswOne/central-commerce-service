package com.jswone.commerce.web.controllers;

import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.model.BreadcrumbData;
import com.jswone.commerce.core.model.CategoryTreeResponse;
import com.jswone.commerce.core.model.request.BulkCategoryRequestDTO;
import com.jswone.commerce.core.service.CatalogueCategoryService;
import com.jswone.commerce.core.util.ApiResponseUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@Log4j2
public class CatalogueCategoryController implements CentralBaseController {

  private final CatalogueCategoryService categoryService;

    public CatalogueCategoryController(CatalogueCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/categories/tree")
  public ApiResponse<CategoryTreeResponse> getCategoryTree() {
    log.info("Received request for category tree");
    CategoryTreeResponse categoryTreeResponse = categoryService.getCatalogueCategoryTree();
    log.info("Successfully fetched category tree");
    return ApiResponseUtil.createSuccessResponse(categoryTreeResponse, HttpStatus.OK);
  }

  @GetMapping("/categories/breadcrumb")
  public ApiResponse<BreadcrumbData> getBreadcrumb(@RequestParam(required = false) String categoryId,
                                                   @RequestParam(required = false) String slug) {
    log.info("Received breadcrumb request for categoryId: {} or slug {}", categoryId, slug);
    BreadcrumbData breadcrumbData = categoryService.getBreadcrumbData(categoryId, slug);
    log.info("Successfully fetched breadcrumb response for categoryId: {} or slug {}", categoryId, slug);
    return ApiResponseUtil.createSuccessResponse(breadcrumbData, HttpStatus.OK);
  }

  @PostMapping("/categories/tree/list")
  public ApiResponse<CategoryTreeResponse> getBulkCategoryTree(@Valid @RequestBody BulkCategoryRequestDTO categoryRequestDTO) {
    log.info("Received request for bulk category tree : {}",categoryRequestDTO);
    CategoryTreeResponse categoryTreeResponse = categoryService.getBulkCatalogueCategoryTree(categoryRequestDTO);
    log.info("Successfully fetched bulk category tree");
    return ApiResponseUtil.createSuccessResponse(categoryTreeResponse, HttpStatus.OK);
  }
}
