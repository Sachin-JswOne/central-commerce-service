package com.jswone.commerce.web.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jswone.commerce.core.model.CategoryTreeResponse;
import java.io.File;
import java.io.IOException;

public class CatalogueTestWebUtil {

  public static CategoryTreeResponse getCategoryTree() throws IOException {
    ObjectMapper mapper = getObjectMapper();
    return mapper.readValue(
        new File("src/test/resources/CategoryTree.json"), CategoryTreeResponse.class);
  }

  public static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }
}
