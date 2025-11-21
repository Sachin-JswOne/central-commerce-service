package com.jswone.commerce.core.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jswone.commerce.core.model.CatalogueAttributes;
import com.jswone.commerce.core.model.CatalogueCategoryTree;
import com.jswone.commerce.core.model.NavigationItem;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class CategoryMapperTest {

  private final CategoryMapper mapper = Mappers.getMapper(CategoryMapper.class);

  @Test
  void testMapCategory_AllFields() {
    CatalogueCategoryTree source = new CatalogueCategoryTree();
    source.setKey("Steel");

    CatalogueAttributes attributes = new CatalogueAttributes();
    attributes.setSeo_url("steel-seo");
    attributes.setMeta_title("Steel Title");
    attributes.setMeta_description("Steel Description");
    attributes.setSlug("steel-slug");
    attributes.setCategory_title("Steel");
    source.setAttributes(attributes);

    NavigationItem result = mapper.mapCategory(source);

    assertEquals("Steel", result.getName());
    assertEquals("steel-seo", result.getSeoUrl());
    assertEquals("Steel Title", result.getMetaTitle());
    assertEquals("Steel Description", result.getMetaDescription());
    assertEquals("steel-slug", result.getSlug());
  }

  @Test
  void testMapCategory_NullAttributes() {
    CatalogueCategoryTree source = new CatalogueCategoryTree();
    source.setKey("Steel");
    source.setAttributes(null);

    NavigationItem result = mapper.mapCategory(source);

    assertNull(result.getSeoUrl());
    assertNull(result.getMetaTitle());
  }

  @Test
  void testMapCategories_EmptyList() {
    List<NavigationItem> result = mapper.mapCategories(Collections.emptyList());
    assertTrue(result.isEmpty());
  }
}
