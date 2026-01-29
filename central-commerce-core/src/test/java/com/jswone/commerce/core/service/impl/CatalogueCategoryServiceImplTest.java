package com.jswone.commerce.core.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.mapper.CategoryMapper;
import com.jswone.commerce.core.model.CatalogueCategoryTreeResponse;
import com.jswone.commerce.core.model.CategoryTreeResponse;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.util.CatalogueTestUtilCore;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class CatalogueCategoryServiceImplTest {

  @InjectMocks private CatalogueCategoryServiceImpl service;

  @Mock
  CentralCatalogueClient centralCatalogueClient;

  @Mock CategoryMapper categoryMapper;

  @Test
  void testGetCatalogueCategoryTree_Success() throws IOException {
    CatalogueCategoryTreeResponse mockCatalogueResponse =
        CatalogueTestUtilCore.getCatalogueCategoryTree();

    CategoryTreeResponse categoryTreeResponse = CatalogueTestUtilCore.getCategoryTree();

    Mockito.when(centralCatalogueClient.getCategoryTree())
        .thenReturn(mockCatalogueResponse.getData());
    Mockito.when(categoryMapper.mapCategories(Mockito.anyList()))
        .thenReturn(categoryTreeResponse.getNavigation());

    CategoryTreeResponse response = service.getCatalogueCategoryTree();

    assertNotNull(response);
    assertEquals("Steel", response.getNavigation().getFirst().getName());
  }

  @Test
  void testGetCatalogueCategoryTree_NullResponse() {
    Mockito.when(centralCatalogueClient.getCategoryTree()).thenReturn(null);

    CentralCommerceServiceException ex =
        assertThrows(
            CentralCommerceServiceException.class, () -> service.getCatalogueCategoryTree());

    assertEquals("Category tree API returned invalid or empty data", ex.getMessage());
  }

  @Test
  void testGetCatalogueCategoryTree_EmptyData() {
    CatalogueCategoryTreeResponse emptyResponse = new CatalogueCategoryTreeResponse();
    emptyResponse.setData(null);

    Mockito.when(centralCatalogueClient.getCategoryTree()).thenReturn(emptyResponse.getData());

    CentralCommerceServiceException ex =
        assertThrows(
            CentralCommerceServiceException.class, () -> service.getCatalogueCategoryTree());

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getHttpStatus());
  }
}
