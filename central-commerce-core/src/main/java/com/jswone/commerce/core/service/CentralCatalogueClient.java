package com.jswone.commerce.core.service;

import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.constants.GeneralConstants;
import com.jswone.commerce.core.exceptions.CentralCatalogueServiceException;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.*;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class CentralCatalogueClient {

  @Autowired private RestTemplate restTemplate;

  @Autowired private CommerceValueConfig commerceValueConfig;

  public List<CatalogueCategoryTree> getCategoryTree() {
    try {
      log.info(
          "Calling external central catalogue category tree API: {}",
          commerceValueConfig.getCatalogueCategoryBaseUrl());
      HttpHeaders headers = new HttpHeaders();
      headers.set(GeneralConstants.X_API_KEY, commerceValueConfig.getCatalogueCategoryApiKey());
      headers.set(GeneralConstants.CLIENT_ID, commerceValueConfig.getCatalogueCategoryClientId());

      HttpEntity<?> entity = new HttpEntity<>(headers);
      String url = commerceValueConfig.getCatalogueCategoryBaseUrl().concat("/category-tree");

      ResponseEntity<CatalogueCategoryTreeResponse> response =
          restTemplate.exchange(url, HttpMethod.GET, entity, CatalogueCategoryTreeResponse.class);

      if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
        log.error("Failed to fetch catalogue category tree data: {}", response.getStatusCode());
        throw new CentralCommerceServiceException(
            "Failed to fetch catalogue category tree ", (HttpStatus) response.getStatusCode());
      }

      log.info("Central catalogue tree API call successful");
      return response.getBody() != null ? response.getBody().getData() : Collections.emptyList();

    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      log.error("Error calling catalogue tree API: {}", ex.getMessage(), ex);
      throw new CentralCatalogueServiceException(
          "Error calling catalogue tree API", (HttpStatus) ex.getStatusCode());

    } catch (Exception ex) {
      log.error("Error calling external Catalogue API: {}", ex.getMessage(), ex);
      throw new CentralCommerceServiceException(
          "Error calling central catalogue category tree API: ",
          HttpStatus.INTERNAL_SERVER_ERROR,
          ex);
    }
  }

  public CatalogueBreadCrumbData getBreadcrumb(String categoryId) {
    String url =
        commerceValueConfig.getCatalogueCategoryBaseUrl().concat("/category/").concat(categoryId);
    log.info("Calling Central Catalogue breadcrumb API: {}", url);

    try {
      HttpHeaders headers = new HttpHeaders();
      headers.set(GeneralConstants.X_API_KEY, commerceValueConfig.getCatalogueCategoryApiKey());
      headers.set(GeneralConstants.CLIENT_ID, commerceValueConfig.getCatalogueCategoryClientId());
      HttpEntity<?> entity = new HttpEntity<>(headers);

      ResponseEntity<CatalogueBreadcrumbResponse> response =
          restTemplate.exchange(url, HttpMethod.GET, entity, CatalogueBreadcrumbResponse.class);

      if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
        log.error("Failed to fetch catalogue breadcrumb data: {}", response.getStatusCode());
        throw new CentralCommerceServiceException(
            "Failed to fetch catalogue breadcrumb data", (HttpStatus) response.getStatusCode());
      }

      log.info("Central catalogue breadcrumb API call successful");
      return response.getBody() != null ? response.getBody().getData() : null;

    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      log.error("Error calling catalogue breadcrumb API: {}", ex.getMessage(), ex);
      throw new CentralCatalogueServiceException(
          "Error calling catalogue breadcrumb API", (HttpStatus) ex.getStatusCode());

    } catch (Exception ex) {
      log.error("Error calling catalogue breadcrumb API: {}", ex.getMessage(), ex);
      throw new CentralCommerceServiceException(
          "Error calling central catalogue breadcrumb API", HttpStatus.INTERNAL_SERVER_ERROR, ex);
    }
  }
}
