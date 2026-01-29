package com.jswone.commerce.web.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;

import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.model.CategoryTreeResponse;
import com.jswone.commerce.core.service.impl.CatalogueCategoryServiceImpl;
import com.jswone.commerce.web.util.CatalogueTestWebUtil;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class CatalogueCategoryControllerTest {

  @InjectMocks private CatalogueCategoryController controller;

  @Mock private CatalogueCategoryServiceImpl categoryService;

  @Test
  void testGetCategoryTree_Success() throws IOException {
    CategoryTreeResponse mockResponse = CatalogueTestWebUtil.getCategoryTree();

    Mockito.when(categoryService.getCatalogueCategoryTree()).thenReturn(mockResponse);

    ApiResponse<CategoryTreeResponse> response = controller.getCategoryTree();

    assertTrue(response.isSuccess());
    assertEquals(HttpStatus.OK, response.getStatus());
    assertEquals("Steel", response.getData().getNavigation().getFirst().getName());
    Mockito.verify(categoryService, times(1)).getCatalogueCategoryTree();
  }

  @Test
  void testGetCategoryTree_Failure() {
    Mockito.when(categoryService.getCatalogueCategoryTree())
        .thenThrow(new CentralCommerceServiceException("Data not found", HttpStatus.NOT_FOUND));

    CentralCommerceServiceException ex =
        assertThrows(CentralCommerceServiceException.class, () -> controller.getCategoryTree());

    assertEquals("Data not found", ex.getMessage());
    assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
  }
}
