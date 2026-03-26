package com.jswone.commerce.core.rest;

import com.jswone.commerce.core.model.CatalogueBreadCrumbData;
import com.jswone.commerce.core.model.CatalogueCategoryTree;
import com.jswone.commerce.core.model.SearchedCategoryTree;
import com.jswone.commerce.core.model.request.ProductBulkRequest;
import com.jswone.commerce.core.model.request.ProductTypeBulkRequest;
import com.jswone.commerce.core.model.request.ProductListingRequest;
import com.jswone.commerce.core.model.request.Search.SearchRequest;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductBulkResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductListingCatalogueResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductSearchResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductTypeBulkResponse;

import java.util.List;
import java.util.Set;

public interface CentralCatalogueClient {
    ProductSearchResponse genericSearch(SearchRequest searchRequest);
    ProductSearchResponse genericSearchFacetsOnly(SearchRequest searchRequest);
    ProductBulkResponse bulkMMIDResponse(ProductBulkRequest productBulkRequest);
    ProductTypeBulkResponse bulkTypeIdResponse(ProductTypeBulkRequest productTypeBulkRequest);
    ProductListingCatalogueResponse productListing(ProductListingRequest productListingRequest);
    ProductListingCatalogueResponse productListingFacetsOnly(ProductListingRequest productListingRequest);
    List<CatalogueCategoryTree> getCategoryTree();
    CatalogueBreadCrumbData getBreadcrumb(String categoryId, String slug);
    ProductBulkResponse getProductFromSlug(String slug, String storeFront);
    SearchedCategoryTree getSearchedCategoryTree(Set<String> categoryIds);
}
