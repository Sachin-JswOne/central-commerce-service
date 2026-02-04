package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.enums.seo.CategoryType;
import com.jswone.commerce.core.enums.seo.SeoEntityType;
import com.jswone.commerce.core.enums.seo.SeoOperationType;
import com.jswone.commerce.core.enums.seo.SeoPageType;
import com.jswone.commerce.core.factory.SeoPatternFactory;
import com.jswone.commerce.core.model.CatalogueCategoryTree;
import com.jswone.commerce.core.model.centralCatalogue.Product;
import com.jswone.commerce.core.model.centralCatalogue.ProductTypeData;
import com.jswone.commerce.core.model.centralCatalogue.Variant;
import com.jswone.commerce.core.model.seo.CategoryIdentifier;
import com.jswone.commerce.core.model.seo.CategoryResponse;
import com.jswone.commerce.core.model.seo.ProductResponse;
import com.jswone.commerce.core.model.seo.UrlGroup;
import com.jswone.commerce.core.model.seo.VariantResponse;
import com.jswone.commerce.core.model.request.ProductTypeBulkRequest;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductTypeBulkResponse;
import com.jswone.commerce.core.model.seo.SeoContext;
import com.jswone.commerce.core.model.seo.SeoData;
import com.jswone.commerce.core.model.seo.SeoMeta;
import com.jswone.commerce.core.model.seo.UrlMeta;
import com.jswone.commerce.core.pattern.SeoPatternHandler;
import com.jswone.commerce.core.resolver.SeoContextResolver;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.service.SeoService;
import com.jswone.commerce.core.constants.CacheNames;
import com.jswone.commerce.core.util.CatalogueUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultSeoService implements SeoService {

        private final SeoPatternFactory patternFactory;
        private final SeoContextResolver contextResolver;
        private final CentralCatalogueClient centralCatalogueClient;
        private final CacheManager cacheManager;

        /*
         * SITEMAP GENERATION
         */

        @Override
        public List<CategoryResponse> generateSitemap() {
                log.info("Starting sitemap generation...");
                List<CategoryResponse> sitemap = new ArrayList<>();
                List<CategoryIdentifier> categoryIdentifierList = fetchAllCategoryIdsFromCC();
                log.info("Found {} categories to process", categoryIdentifierList.size());

                for (CategoryIdentifier category : categoryIdentifierList) {
                        log.info("Processing category: {} ({})", category.getCategorySlug(),
                                        category.getCategoryType());
                        sitemap.add(processCategory(category.getCategoryId(), category.getCategorySlug(),
                                        category.getCategoryType()));
                }

                log.info("Sitemap generation complete. Total categories: {}", sitemap.size());
                return sitemap;
        }

        private CategoryResponse processCategory(String categoryId, String categorySlug, CategoryType categoryType) {

                // Initialize cache for this category
                initializeCategoryLocationCache(categoryId);

                SeoContext categoryContext = SeoContext.builder()
                                .entityType(SeoEntityType.CATEGORY)
                                .pageType(SeoPageType.PLP)
                                .categoryType(categoryType)
                                .categoryId(categoryId)
                                .slug(categorySlug)
                                .operationType(SeoOperationType.URL_GENERATION)
                                .build();

                SeoPatternHandler handler = patternFactory.resolve(categoryContext);

                // For BRAND categories: track locations but don't return product URLs
                if (categoryType == CategoryType.BRAND) {
                        // Fetch products to populate location cache, but don't generate product URLs
                        processProducts(categoryId);

                        // Retrieve cached locations after processing products
                        Set<String> cachedLocations = getLocationsFromCache(categoryId);
                        log.debug("Retrieved {} locations from cache for BRAND category: {}", cachedLocations.size(),
                                        categoryId);

                        UrlGroup categoryUrls = buildCategoryUrls(categoryContext, handler, cachedLocations);

                        // Clear cache for this category after URL generation is complete
                        clearCategoryLocationCache(categoryId);

                        return new CategoryResponse(
                                        categoryId,
                                        categoryType,
                                        categorySlug,
                                        categoryUrls,
                                        List.of()); // Empty product list for BRAND categories
                }

                // For STANDARD categories: process products and return product URLs
                List<ProductResponse> products = processProducts(categoryId);

                // Retrieve cached locations after processing all products
                Set<String> cachedLocations = getLocationsFromCache(categoryId);
                log.debug("Retrieved {} locations from cache for category: {}", cachedLocations.size(), categoryId);

                UrlGroup categoryUrls = buildCategoryUrls(categoryContext, handler, cachedLocations);

                // Clear cache for this category after URL generation is complete
                clearCategoryLocationCache(categoryId);

                return new CategoryResponse(
                                categoryId,
                                categoryType,
                                categorySlug,
                                categoryUrls,
                                products);
        }

        private List<ProductResponse> processProducts(String categoryId) {

                log.debug("Fetching products for category: {}", categoryId);
                List<Product> productsFromCC = centralCatalogueClient.getAllProductsForCategoryId(categoryId, "msme");

                if (productsFromCC == null || productsFromCC.isEmpty()) {
                        log.debug("No products found for category: {}", categoryId);
                        return List.of();
                }

                log.info("Processing {} products for category: {}", productsFromCC.size(), categoryId);

                // Fetch product types in bulk for all products
                Set<String> productTypeIds = productsFromCC.stream()
                                .map(Product::getProductTypeId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());

                Map<String, ProductTypeData> productTypeMap = new HashMap<>();
                if (!productTypeIds.isEmpty()) {
                        try {
                                ProductTypeBulkRequest typeRequest = new ProductTypeBulkRequest(
                                                productTypeIds,
                                                "msme");
                                ProductTypeBulkResponse typeResponse = centralCatalogueClient
                                                .bulkTypeIdResponse(typeRequest);

                                if (typeResponse != null
                                                && typeResponse.getData() != null
                                                && typeResponse.getData().getProductTypeDetail() != null) {
                                        productTypeMap = typeResponse.getData().getProductTypeDetail();
                                        log.debug("Fetched {} product types for category: {}", productTypeMap.size(),
                                                        categoryId);
                                }
                        } catch (Exception e) {
                                log.error("Failed to fetch product types in bulk for category: {}", categoryId, e);
                        }
                }

                List<ProductResponse> productResponses = new ArrayList<>();

                for (Product product : productsFromCC) {

                        // Null safety: skip products without valid attributes
                        if (product.getAttributes() == null || product.getAttributes().get("slug") == null) {
                                log.warn("Skipping product {} - missing slug attribute", product.getId());
                                continue;
                        }

                        String productBaseSlug = CatalogueUtil.str(product.getAttributes().get("slug"));

                        // Build base product context
                        Instant productLastMod = parseLastModified(product.getLastModifiedAt());

                        SeoContext productBaseContext = SeoContext.builder()
                                        .entityType(SeoEntityType.PRODUCT)
                                        .pageType(SeoPageType.PDP)
                                        .categoryType(CategoryType.STANDARD)
                                        .productId(product.getId())
                                        .slug(productBaseSlug)
                                        .lastModifiedAt(productLastMod)
                                        .operationType(SeoOperationType.URL_GENERATION)
                                        .build();

                        SeoPatternHandler handler = patternFactory.resolve(productBaseContext);

                        // Base product URL
                        UrlMeta baseUrl = handler.generateUrl(productBaseContext);

                        // Location URLs (derived from product locations)
                        Map<String, UrlMeta> locationUrls = new HashMap<>();

                        List<String> productLocationList = (product.getProductLocation() != null)
                                        ? product.getProductLocation()
                                                        .stream()
                                                        .flatMap(location -> Stream.of(
                                                                        location.getState(),
                                                                        location.getDistrict()))
                                                        .filter(loc -> loc != null && !loc.isBlank())
                                                        .distinct()
                                                        .toList()
                                        : List.of();

                        // Add locations to cache for category URL generation
                        for (String location : productLocationList) {
                                // Skip 'all' location
                                if ("all".equalsIgnoreCase(location)) {
                                        continue;
                                }
                                addLocationToCache(categoryId, location);

                                SeoContext locationContext = SeoContext.builder()
                                                .entityType(SeoEntityType.PRODUCT)
                                                .pageType(SeoPageType.PDP)
                                                .categoryType(CategoryType.STANDARD)
                                                .productId(product.getId())
                                                .slug(productBaseSlug).location(location)
                                                .lastModifiedAt(productLastMod)
                                                .operationType(SeoOperationType.URL_GENERATION)
                                                .build();

                                UrlMeta locationUrl = handler.generateUrl(locationContext);
                                locationUrls.put(location, locationUrl);
                        }

                        UrlGroup productUrls = new UrlGroup(baseUrl, locationUrls);

                        // Variants (location-only URLs)
                        List<VariantResponse> variants = new ArrayList<>();

                        // Check if product has type ID - required for variant URL generation
                        if (product.getProductTypeId() == null || product.getProductTypeId().isBlank()) {
                                log.error("Skipping variant URL generation for product {} - missing product type ID",
                                                product.getId());
                        } else if (product.getVariants() != null && !product.getVariants().isEmpty()) {
                                // Get product type data for variant selectors
                                ProductTypeData productTypeData = productTypeMap.get(product.getProductTypeId());

                                if (productTypeData == null) {
                                        log.warn("Product type data not found for typeId: {} (product: {})",
                                                        product.getProductTypeId(), product.getId());
                                }

                                for (Variant variant : product.getVariants()) {

                                        Map<String, UrlMeta> variantLocationUrls = new HashMap<>();

                                        for (String location : productLocationList) {
                                                // Skip 'all' location
                                                if ("all".equalsIgnoreCase(location)) {
                                                        continue;
                                                }

                                                String variantBaseSlug = buildVariantAttributeSlug(product, variant,
                                                                productTypeData);

                                                SeoContext variantContext = SeoContext.builder()
                                                                .entityType(SeoEntityType.VARIANT)
                                                                .pageType(SeoPageType.PDP)
                                                                .categoryType(CategoryType.STANDARD)
                                                                .productId(product.getId())
                                                                .slug(variantBaseSlug)
                                                                .variantMmid(variant.getVariantMmid())
                                                                .location(location)
                                                                .lastModifiedAt(productLastMod)
                                                                .operationType(SeoOperationType.URL_GENERATION)
                                                                .build();

                                                UrlMeta variantUrl = handler.generateUrl(variantContext);
                                                variantLocationUrls.put(location, variantUrl);
                                        }

                                        if (!variantLocationUrls.isEmpty()) {
                                                variants.add(
                                                                new VariantResponse(
                                                                                variant.getVariantMmid(),
                                                                                new UrlGroup(null,
                                                                                                variantLocationUrls)));
                                        }
                                }
                        }

                        productResponses.add(
                                        new ProductResponse(
                                                        product.getId(),
                                                        productBaseSlug,
                                                        productUrls,
                                                        variants));
                }

                log.info("Completed processing {} products for category: {}", productResponses.size(), categoryId);
                return productResponses;
        }

        private String buildVariantAttributeSlug(Product product, Variant variant, ProductTypeData productTypeData) {

                String baseSlug = CatalogueUtil.str(product.getAttributes().get("slug"));

                // If no variant attributes, return base slug
                if (variant.getAttributes() == null || variant.getAttributes().isEmpty()) {
                        return baseSlug;
                }

                // If no product type data or variant selectors, fall back to all attributes
                if (productTypeData == null || productTypeData.getVariantSelectors() == null
                                || productTypeData.getVariantSelectors().isEmpty()) {
                        log.warn("No variant selectors found for product {}, using all variant attributes for slug",
                                        product.getId());
                        return buildSlugWithAllAttributes(baseSlug, variant);
                }

                // Use only attributes specified in variant selectors
                Set<String> selectorKeys = productTypeData.getVariantSelectors().keySet();
                String attributePart = variant.getAttributes().entrySet()
                                .stream()
                                .filter(e -> selectorKeys.contains(e.getKey())) // Only include attributes in selectors
                                .sorted(Map.Entry.comparingByKey()) // stable URLs
                                .map(e -> e.getKey().toLowerCase() + "-"
                                                + e.getValue().toLowerCase().replaceAll("\\s+", "-"))
                                .collect(Collectors.joining("-"));

                return attributePart.isEmpty() ? baseSlug : baseSlug + "-" + attributePart;
        }

        /**
         * Fallback method to build slug with all variant attributes
         */
        private String buildSlugWithAllAttributes(String baseSlug, Variant variant) {
                String attributePart = variant.getAttributes().entrySet()
                                .stream()
                                .sorted(Map.Entry.comparingByKey()) // stable URLs
                                .map(e -> e.getKey().toLowerCase() + "-"
                                                + e.getValue().toLowerCase().replaceAll("\\s+", "-"))
                                .collect(Collectors.joining("-"));

                return attributePart.isEmpty() ? baseSlug : baseSlug + "-" + attributePart;
        }

        private UrlGroup buildCategoryUrls(
                        SeoContext context,
                        SeoPatternHandler handler,
                        Set<String> locations) {
                UrlMeta base = handler.generateUrl(context);

                Map<String, UrlMeta> locationUrls = new HashMap<>();

                // Generate location-based category URLs from cached locations
                for (String location : locations) {
                        // Skip 'all' location
                        if ("all".equalsIgnoreCase(location)) {
                                continue;
                        }
                        SeoContext locationContext = SeoContext.builder()
                                        .entityType(context.getEntityType())
                                        .pageType(context.getPageType())
                                        .categoryType(context.getCategoryType())
                                        .categoryId(context.getCategoryId())
                                        .slug(context.getSlug())
                                        .location(location)
                                        .operationType(SeoOperationType.URL_GENERATION)
                                        .build();

                        UrlMeta locationUrl = handler.generateUrl(locationContext);
                        locationUrls.put(location, locationUrl);
                }

                return new UrlGroup(base, locationUrls);
        }

        /*
         * CAFFEINE CACHE HELPERS
         */

        /**
         * Initialize an empty location set for a category in cache
         */
        private void initializeCategoryLocationCache(String categoryId) {
                Cache cache = cacheManager.getCache(CacheNames.SEO_CATEGORY_LOCATIONS);
                if (cache != null) {
                        cache.put(categoryId, new HashSet<String>());
                        log.debug("Initialized location cache for category: {}", categoryId);
                } else {
                        log.warn("SEO_CATEGORY_LOCATIONS cache not found");
                }
        }

        /**
         * Add a location to the category's cached location set
         */
        private void addLocationToCache(String categoryId, String location) {
                Cache cache = cacheManager.getCache(CacheNames.SEO_CATEGORY_LOCATIONS);
                if (cache != null) {
                        @SuppressWarnings("unchecked")
                        Set<String> locations = cache.get(categoryId, HashSet.class);
                        if (locations != null) {
                                locations.add(location);
                                cache.put(categoryId, locations);
                        }
                }
        }

        /**
         * Parse last modified timestamp from product (ISO 8601 format)
         */
        private Instant parseLastModified(String lastModifiedAt) {
                if (lastModifiedAt == null || lastModifiedAt.isEmpty()) {
                        return null;
                }
                try {
                        return Instant.parse(lastModifiedAt);
                } catch (Exception e) {
                        log.warn("Failed to parse lastModifiedAt: {}", lastModifiedAt);
                        return null;
                }
        }

        /**
         * Retrieve all cached locations for a category
         */
        private Set<String> getLocationsFromCache(String categoryId) {
                Cache cache = cacheManager.getCache(CacheNames.SEO_CATEGORY_LOCATIONS);
                if (cache != null) {
                        @SuppressWarnings("unchecked")
                        Set<String> locations = cache.get(categoryId, HashSet.class);
                        return locations != null ? locations : Set.of();
                }
                return Set.of();
        }

        /**
         * Clear cached locations for a category after processing is complete
         */
        private void clearCategoryLocationCache(String categoryId) {
                Cache cache = cacheManager.getCache(CacheNames.SEO_CATEGORY_LOCATIONS);
                if (cache != null) {
                        cache.evict(categoryId);
                        log.debug("Cleared location cache for category: {}", categoryId);
                }
        }

        /*
         * RUNTIME — SEO METADATA RESOLUTION
         */
        @Override
        public SeoMeta resolveSeoMeta(
                        String path,
                        Map<String, String> pathVariables) {

                SeoContext context = contextResolver.resolve(path, pathVariables);

                SeoPatternHandler handler = patternFactory.resolve(context);

                SeoData seoData = handler.fetchData(context);

                return handler.generateMeta(context, seoData);
        }

        private List<CategoryIdentifier> fetchAllCategoryIdsFromCC() {

                List<CatalogueCategoryTree> categoryTree = centralCatalogueClient.getCategoryTree();

                if (categoryTree == null || categoryTree.isEmpty()) {
                        return List.of();
                }

                List<CategoryIdentifier> result = new ArrayList<>();

                for (CatalogueCategoryTree root : categoryTree) {

                        CategoryType categoryType = "Brands".equalsIgnoreCase(root.getKey())
                                        ? CategoryType.BRAND
                                        : CategoryType.STANDARD;

                        if (root.getSub_menu() != null) {
                                for (CatalogueCategoryTree child : root.getSub_menu()) {
                                        collectSubCategories(child, categoryType, result);
                                }
                        }
                }

                return result;
        }

        private void collectSubCategories(
                        CatalogueCategoryTree node,
                        CategoryType categoryType,
                        List<CategoryIdentifier> result) {

                if (node.getId() != null
                                && node.getAttributes() != null
                                && node.getAttributes().getSlug() != null) {

                        result.add(
                                        CategoryIdentifier.builder()
                                                        .categoryId(node.getId())
                                                        .categorySlug(node.getAttributes().getSlug())
                                                        .categoryType(categoryType)
                                                        .build());
                }

                if (node.getSub_menu() == null || node.getSub_menu().isEmpty()) {
                        return;
                }

                for (CatalogueCategoryTree child : node.getSub_menu()) {
                        collectSubCategories(child, categoryType, result);
                }
        }
}
