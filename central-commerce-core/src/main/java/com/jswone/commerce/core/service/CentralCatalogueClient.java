package com.jswone.commerce.core.service;

import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.CatalogueBreadCrumbData;
import com.jswone.commerce.core.model.CatalogueBreadcrumbResponse;
import com.jswone.commerce.core.model.CatalogueCategoryTreeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class CentralCatalogueClient {

  @Autowired private RestTemplate restTemplate;

  @Autowired private CommerceValueConfig commerceValueConfig;

  public CatalogueCategoryTreeResponse getCatalogueCategoryTree() {
    try {
      log.info(
          "Calling external central catalogue category tree API: {}",
          commerceValueConfig.getCatalogueCategoryBaseUrl());
      HttpHeaders headers = new HttpHeaders();
      headers.set("X-API-KEY", commerceValueConfig.getCatalogueCategoryApiKey());
      headers.set("CLIENT-ID", commerceValueConfig.getCatalogueCategoryClientId());

      HttpEntity<?> entity = new HttpEntity<>(headers);

      ResponseEntity<CatalogueCategoryTreeResponse> response =
          restTemplate.exchange(
              commerceValueConfig.getCatalogueCategoryBaseUrl().concat("category-tree"),
              HttpMethod.GET,
              entity,
              CatalogueCategoryTreeResponse.class);
      if (response.getStatusCode() != HttpStatus.OK) {
        log.error("Failed to fetch catalogue category tree data: {}", response.getStatusCode());
        throw new CentralCommerceServiceException(
            "Failed to fetch catalogue category tree " + response.getStatusCode());
      }
      log.info("Central catalogue tree API call successful");
      return response.getBody();
    } catch (Exception ex) {
      log.error("Error calling external Catalogue API: {}", ex.getMessage(), ex);
      throw new CentralCommerceServiceException(
          "Error calling central catalogue category tree API: ",
          HttpStatus.INTERNAL_SERVER_ERROR,
          ex);
    }
  }

  public CatalogueBreadCrumbData fetchBreadcrumb(String categoryId) {
    String url =
        commerceValueConfig.getCatalogueCategoryBaseUrl() + "/category/categoryId=" + categoryId;

    log.info("Calling Catalogue breadcrumb API: {}", url);

    try {
      HttpHeaders headers = new HttpHeaders();
      headers.set("X-API-KEY", commerceValueConfig.getCatalogueCategoryApiKey());
      headers.set("CLIENT-ID", commerceValueConfig.getCatalogueCategoryClientId());

      HttpEntity<?> entity = new HttpEntity<>(headers);

      ResponseEntity<CatalogueBreadcrumbResponse> response =
          restTemplate.exchange(url, HttpMethod.POST, entity, CatalogueBreadcrumbResponse.class);

      if (response.getStatusCode() != HttpStatus.OK
          || response.getBody() == null
          || response.getBody().getData() == null) {
        log.error("Failed to fetch catalogue breadcrumb data: {}", response.getStatusCode());
        throw new CentralCommerceServiceException(
            "Failed to fetch catalogue breadcrumb " + response.getStatusCode());
      }
      log.info("Central catalogue breadcrumb API call successful");

      return response.getBody().getData();
    } catch (Exception ex) {
      log.error("Error calling external catalogue breadcrumb API: {}", ex.getMessage(), ex);
      throw new CentralCommerceServiceException(
          "Error calling central catalogue breadcrumb API: ", HttpStatus.INTERNAL_SERVER_ERROR, ex);
    }
  }
}
