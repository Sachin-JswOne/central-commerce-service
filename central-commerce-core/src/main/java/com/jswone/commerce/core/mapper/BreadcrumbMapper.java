package com.jswone.commerce.core.mapper;

import com.jswone.commerce.core.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BreadcrumbMapper {
  @Mapping(source = "url_key", target = "urlKey")
  @Mapping(source = "bread_crumb", target = "breadCrumb")
  @Mapping(source = "description", target = "description")
  @Mapping(source = "bread_crumb_details", target = "breadCrumbDetailList")
  BreadcrumbData toBreadcrumbResponse(CatalogueBreadCrumbData catalogueResponse);

  @Mapping(source = "attributes.category_title", target = "name")
  @Mapping(source = "id", target = "categoryId")
  @Mapping(source = "key", target = "categoryKey")
  @Mapping(source = "attributes.slug", target = "slug")
  @Mapping(source = "attributes.category_description", target = "description")
  @Mapping(source = "attributes.category_detail_description", target = "detailDescription")
  @Mapping(source = "attributes.category_content_heading", target = "categoryContentHeading")
  @Mapping(
      source = "attributes.category_content_description",
      target = "categoryContentDescription")
  @Mapping(source = "attributes.category_faqs_heading", target = "faqsHeading")
  @Mapping(source = "attributes.category_faqs_description", target = "faqsDescription")
  @Mapping(source = "attributes.meta_image", target = "metaImage")
  @Mapping(source = "attributes.seo_meta", target = "seoMeta")
  BreadcrumbDetail toBreadcrumbDetail(CatalogueBreadCrumbDetail detail);

  @Mapping(source = "title", target = "name")
  @Mapping(source = "asset_id", target = "id")
  @Mapping(source = "alt_text", target = "alternativeText")
  @Mapping(source = "content_type", target = "contentType")
  @Mapping(source = "public_url", target = "url")
  MetaImage toMetaImage(CatalogueMetaImage metaImage);
}
