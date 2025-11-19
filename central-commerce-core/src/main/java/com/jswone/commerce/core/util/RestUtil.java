package com.jswone.commerce.core.util;

import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.slf4j.MDC;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@Log4j2
public class RestUtil {

  private final RestTemplate commerceRestTemplate;

  public RestUtil(RestTemplate commerceRestTemplate) {
    this.commerceRestTemplate = commerceRestTemplate;
  }

  public <T> ResponseEntity<T> makeRestCall(
      String url,
      Object requestBody,
      HttpMethod httpMethod,
      Object responseType,
      Map<String, String> headersMap) {
    MultiValueMap<String, String> headers = new HttpHeaders();

    if (headersMap != null) {
      headers.setAll(headersMap);
    }
    headers.add("traceId", MDC.get("traceId"));
    headers.add("appName", "cart-service");

    // Create the HttpEntity with request body and headers
    HttpEntity<Object> httpEntity = new HttpEntity<>(requestBody, headers);

    log.info("Calling rest API: {} with method: {}", url, httpMethod);

    // Make the HTTP request
    if (responseType instanceof Class<?>) {
      return commerceRestTemplate.exchange(url, httpMethod, httpEntity, (Class<T>) responseType);
    } else if (responseType instanceof ParameterizedTypeReference<?>) {
      return commerceRestTemplate.exchange(
          url, httpMethod, httpEntity, (ParameterizedTypeReference<T>) responseType);
    } else {
      throw new IllegalArgumentException("Unsupported response type");
    }
  }
}
