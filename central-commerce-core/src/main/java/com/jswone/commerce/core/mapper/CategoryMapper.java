package com.jswone.commerce.core.mapper;

import com.jswone.commerce.core.model.CatalogueCategoryTree;
import com.jswone.commerce.core.model.NavigationItem;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

// @Mapper(componentModel = "spring")
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

  @Mapping(source = "key", target = "name")
  @Mapping(source = "attributes.seo_url", target = "seoUrl")
  @Mapping(source = "attributes.meta_title", target = "metaTitle")
  @Mapping(source = "attributes.meta_description", target = "metaDescription")
  @Mapping(source = "attributes.slug", target = "slug")
  @Mapping(source = "attributes.href", target = "href")
  @Mapping(target = "linkTitle", source = "key")
  @Mapping(target = "subMenu", expression = "java(mapCategories(category.getSub_menu()))")
  NavigationItem mapCategory(CatalogueCategoryTree category);

  default List<NavigationItem> mapCategories(List<CatalogueCategoryTree> categories) {
    if (categories == null) return Collections.emptyList();
    return categories.stream().map(this::mapCategory).collect(Collectors.toList());
  }
}
