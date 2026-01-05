package com.jswone.commerce.web.controllers;

import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.model.ImageMetadata;
import com.jswone.commerce.core.model.request.ProductListingRequest;
import com.jswone.commerce.core.model.request.Search.SearchRequest;
import com.jswone.commerce.core.model.response.ProductListingResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductBulkResponse;
import com.jswone.commerce.core.model.response.search.SearchResponse;
import com.jswone.commerce.core.service.CentralCatalogueService;
import com.jswone.commerce.core.util.ApiResponseUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@Slf4j
public class CatalogueController implements CentralBaseController {

    private final CentralCatalogueService centralCatalogueService;

    public CatalogueController(CentralCatalogueService centralCatalogueService) {
        this.centralCatalogueService = centralCatalogueService;
    }

    @PostMapping(value = "/catalogue/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<SearchResponse> searchCatalogue(@Valid @RequestBody SearchRequest searchRequest) {
        log.info("Received request for generic search :{} ", searchRequest.toString());
        return ApiResponseUtil.createSuccessResponse(centralCatalogueService.searchCatalogue(searchRequest), HttpStatus.OK);
    }

    @PostMapping("/catalogue/images")
    public ApiResponse<Map<String, ImageMetadata>> fetchBulkImages(@RequestBody @NotEmpty(message = "MMIDs cannot be empty") Set<String> productMmIds) {
        Map<String, ImageMetadata> response = centralCatalogueService.fetchImagesForMmIds(productMmIds);
        return ApiResponseUtil.createSuccessResponse(response, HttpStatus.OK);
    }

    @PostMapping(value = "/products/listing", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<ProductListingResponse> productListing(@Valid @RequestBody ProductListingRequest productListingRequest) {
        log.info("Received request for product listing :{} ", productListingRequest.toString());
        return ApiResponseUtil.createSuccessResponse(centralCatalogueService.productListing(productListingRequest), HttpStatus.OK);
    }

    @PostMapping(value = "/catalogue/product-details", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<ProductBulkResponse> fetchProductDetails(@RequestBody @NotEmpty(message = "MMIDs cannot be empty") Set<String> productMMIDs) {
        log.info("Received request to fetch product details for Product MMIDs: {} ", productMMIDs);
        return ApiResponseUtil.createSuccessResponse(centralCatalogueService.fetchProductsByProductMMIDs(productMMIDs), HttpStatus.OK);
    }
}
