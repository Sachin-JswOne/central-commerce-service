package com.jswone.commerce.web.controllers;

import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.model.BreadcrumbData;
import com.jswone.commerce.core.model.CategoryTreeResponse;
import com.jswone.commerce.core.service.impl.CatalogueCategoryServiceImpl;
import com.jswone.commerce.core.util.ApiResponseUtil;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
public class CatalogueCategoryController implements CentralBaseController {

  @Autowired private CatalogueCategoryServiceImpl categoryService;

  @GetMapping("/categories/tree")
  public ApiResponse<CategoryTreeResponse> getCategoryTree() {
    log.info("Received request for category tree");
    CategoryTreeResponse categoryTreeResponse = categoryService.getCatalogueCategoryTree();
    log.info("Successfully fetched category tree");
    return ApiResponseUtil.createSuccessResponse(categoryTreeResponse, HttpStatus.OK);
  }

  @GetMapping("/categories/breadcrumb")
  public ApiResponse<BreadcrumbData> getBreadcrumb(@NonNull @RequestParam String categoryId) {
    log.info("Received breadcrumb request for categoryId: {}", categoryId);
    BreadcrumbData breadcrumbData = categoryService.getBreadcrumbData(categoryId);
    log.info("Successfully fetched breadcrumb response for categoryId: {}", categoryId);
    return ApiResponseUtil.createSuccessResponse(breadcrumbData, HttpStatus.OK);
  }
}
