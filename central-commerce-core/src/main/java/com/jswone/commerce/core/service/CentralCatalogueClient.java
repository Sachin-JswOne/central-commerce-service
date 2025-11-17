package com.jswone.commerce.core.service;

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

  //  @Value("${catalogue.api.url}")
  //  private String catalogueApiUrl;
  //
  //  public CatalogueCategoryTreeResponse getCatalogueCategoryTree() {
  //    try {
  //      log.info("Calling external central catalogue category tree API: {}", catalogueApiUrl);
  //      HttpHeaders headers = new HttpHeaders();
  //      headers.set("x-api-key", "your-api-key");
  //      headers.set("client-id", "your-client-id");
  //
  //      HttpEntity<?> entity = new HttpEntity<>(headers);
  //
  //      ResponseEntity<CatalogueCategoryTreeResponse> response =
  //          restTemplate.exchange(
  //              catalogueApiUrl, HttpMethod.GET, entity, CatalogueCategoryTreeResponse.class);
  //      if (response.getStatusCode() != HttpStatus.OK) {
  //        log.error("Failed to fetch catalogue category tree data: {}", response.getStatusCode());
  //        throw new CentralCommerceServiceException(
  //            "Failed to fetch catalogue category tree " + response.getStatusCode());
  //      }
  //      log.info("Central catalogue tree API call successful");
  //      return response.getBody();
  //    } catch (Exception ex) {
  //      log.error("Error calling external Catalogue API: {}", ex.getMessage(), ex);
  //      throw new CentralCommerceServiceException(
  //          "Error calling central catalogue category tree API: ",
  //          HttpStatus.INTERNAL_SERVER_ERROR,
  //          ex);
  //    }
  //  }

  // for local testing purpose
  public CatalogueCategoryTreeResponse getCatalogueCategoryTree() {
    return new CatalogueCategoryTreeResponse();
  }
}
