package com.jswone.commerce.core.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jswone.commerce.core.model.CatalogueCategoryTreeResponse;
import com.jswone.commerce.core.model.CategoryTreeResponse;
import java.io.File;
import java.io.IOException;

public class CatalogueTestUtilCore {

  public static CatalogueCategoryTreeResponse getCatalogueCategoryTree() throws IOException {
    ObjectMapper objectMapper = getObjectMapper();
    return objectMapper.readValue(
        new File("src/test/resources/CatalogueCategoryTree.json"),
        CatalogueCategoryTreeResponse.class);
  }

  public static CategoryTreeResponse getCategoryTree() throws IOException {
    ObjectMapper objectMapper = getObjectMapper();
    return objectMapper.readValue(
        new File("src/test/resources/CategoryTree.json"), CategoryTreeResponse.class);
  }

  public static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }
}
