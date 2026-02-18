package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.config.CommerceValueConfig;

import com.jswone.commerce.core.enums.seo.SeoEntityType;
import com.jswone.commerce.core.enums.seo.SeoOperationType;
import com.jswone.commerce.core.enums.seo.SeoPageType;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.exceptions.GcsServiceException;
import com.jswone.commerce.core.factory.SeoPatternFactory;
import com.jswone.commerce.core.model.CatalogueCategoryTree;
import com.jswone.commerce.core.model.centralCatalogue.Product;
import com.jswone.commerce.core.model.centralCatalogue.ProductLocation;
import com.jswone.commerce.core.model.centralCatalogue.ProductTypeData;
import com.jswone.commerce.core.model.centralCatalogue.Variant;
import com.jswone.commerce.core.model.seo.CategoryIdentifier;
import com.jswone.commerce.core.model.seo.CategoryResponse;
import com.jswone.commerce.core.model.seo.ProductResponse;
import com.jswone.commerce.core.model.seo.UrlGroup;
import com.jswone.commerce.core.model.seo.VariantResponse;
import com.jswone.commerce.core.model.seo.SeoContext;
import com.jswone.commerce.core.model.seo.SeoData;
import com.jswone.commerce.core.model.seo.SeoMeta;
import com.jswone.commerce.core.model.seo.UrlMeta;
import com.jswone.commerce.core.pattern.SeoPatternHandler;
import com.jswone.commerce.core.resolver.SeoContextResolver;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.service.SeoService;
import com.jswone.commerce.core.service.GcsService;
import com.jswone.commerce.core.util.SitemapGenerator;
import com.jswone.commerce.core.service.ProductTypeService;
import com.jswone.commerce.core.constants.CacheNames;
import com.jswone.commerce.core.constants.SeoConstants;
import com.jswone.commerce.core.util.CatalogueUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
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
        private final ProductTypeService productTypeService;
        private final CommerceValueConfig commerceValueConfig;

        // Using available processors for optimal performance
        private final ExecutorService executorService = Executors.newFixedThreadPool(
                        Runtime.getRuntime().availableProcessors(),
                        new ThreadFactory() {
                                private final AtomicInteger threadNumber = new AtomicInteger(1);

                                @Override
                                public Thread newThread(Runnable r) {
                                        Thread thread = new Thread(r, "seo-worker-" + threadNumber.getAndIncrement());
                                        thread.setDaemon(true);
                                        return thread;
                                }
                        });

        /*
         * SITEMAP GENERATION
         */

        private final GcsService gcsService;

        @Override
        public boolean generateSitemap() {
                log.info("Starting offline sitemap generation...");

                try {
                        List<CategoryIdentifier> allCategories = fetchAllCategoryIdsFromCC();
                        log.info("Found {} categories to process", allCategories.size());

                        // Dynamic map to accumulate URLs by sitemap file name
                        Map<String, List<UrlMeta>> sitemapUrlMap = new java.util.LinkedHashMap<>();

                        for (CategoryIdentifier cat : allCategories) {
                                try {
                                        CategoryResponse response = processCategory(cat.getCategoryId(),
                                                        cat.getCategorySlug(), cat.getCategoryType());

                                        // Derive sitemap key from category type (e.g., "brands-plp",
                                        // "industry-segments-plp", "categories-plp")
                                        String sitemapKey;
                                        if (SeoConstants.CATEGORY_TYPE_ALL_PRODUCTS
                                                        .equalsIgnoreCase(cat.getCategoryType())) {
                                                sitemapKey = "categories-plp";
                                        } else {
                                                sitemapKey = CatalogueUtil.getSeoUrlCategoryPrefix(
                                                                cat.getCategoryType()) + "-plp";
                                        }

                                        // Collect Category URLs
                                        UrlGroup catUrls = response.urls();
                                        if (catUrls != null && catUrls.getBase() != null) {
                                                sitemapUrlMap.computeIfAbsent(sitemapKey, k -> new ArrayList<>())
                                                                .add(catUrls.getBase());
                                                if (catUrls.getLocations() != null) {
                                                        sitemapUrlMap.get(sitemapKey)
                                                                        .addAll(catUrls.getLocations().values());
                                                }
                                        }

                                        // Collect Product URLs (only for all_products categories)
                                        if (response.products() != null) {
                                                for (ProductResponse prod : response.products()) {
                                                        UrlGroup prodUrls = prod.getUrls();
                                                        if (prodUrls != null && prodUrls.getBase() != null) {
                                                                sitemapUrlMap.computeIfAbsent("pdp-base",
                                                                                k -> new ArrayList<>())
                                                                                .add(prodUrls.getBase());
                                                        }

                                                        if (prod.getStateUrls() != null) {
                                                                sitemapUrlMap.computeIfAbsent("pdp-states",
                                                                                k -> new ArrayList<>())
                                                                                .addAll(prod.getStateUrls().values());
                                                        }

                                                        if (prod.getCityUrls() != null) {
                                                                sitemapUrlMap.computeIfAbsent("pdp-cities",
                                                                                k -> new ArrayList<>())
                                                                                .addAll(prod.getCityUrls().values());
                                                        }

                                                        // Collect Variant URLs
                                                        if (prod.getVariants() != null) {
                                                                for (VariantResponse variant : prod.getVariants()) {
                                                                        UrlGroup vUrls = variant.getUrls();
                                                                        if (vUrls != null && vUrls
                                                                                        .getLocations() != null) {
                                                                                sitemapUrlMap.computeIfAbsent(
                                                                                                "configured-pdp",
                                                                                                k -> new ArrayList<>())
                                                                                                .addAll(vUrls.getLocations()
                                                                                                                .values());
                                                                        }
                                                                }
                                                        }
                                                }
                                        }

                                } catch (Exception e) {
                                        log.error("Failed to process category: {}", cat.getCategoryId(), e);
                                }
                        }

                        List<String> sitemapIndexUrls = new ArrayList<>();
                        // Add hardcoded sitemap entry
                        sitemapIndexUrls.add(commerceValueConfig.getSitemapXmlUrlPrefix() + "/sitemap.xml");

                        // Upload all collected URL groups dynamically
                        for (Map.Entry<String, List<UrlMeta>> entry : sitemapUrlMap.entrySet()) {
                                if (!entry.getValue().isEmpty()) {
                                        int chunkSize = "pdp-cities".equals(entry.getKey()) ? 30000 : 40000;
                                        List<String> urls = uploadChunkedListsToGcs(entry.getValue(), entry.getKey(),
                                                        chunkSize);
                                        sitemapIndexUrls.addAll(urls);
                                }
                        }

                        // Generate Sitemap Index
                        String sitemapIndexXml = SitemapGenerator.generateSitemapIndexXml(sitemapIndexUrls);
                        gcsService.uploadFile(commerceValueConfig.getSeoBucketName(), "index.xml",
                                        new ByteArrayInputStream(sitemapIndexXml.getBytes()),
                                        "application/xml");

                        log.info("Sitemap generation completed successfully.");

                        return true;
                } catch (Exception e) {
                        log.error("Error generating sitemap", e);
                        throw new CentralCommerceServiceException("Sitemap generation failed", e);
                }
        }

        private List<String> uploadChunkedListsToGcs(List<UrlMeta> urls, String fileBaseName, int chunkSize) {
                List<String> uploadedUrls = new ArrayList<>();

                // If total URLs are less than chunk size, just upload one file
                if (urls.size() <= chunkSize) {
                        String fileName = fileBaseName + ".xml.gz";
                        uploadedUrls.add(uploadListToGcs(urls, fileName));
                        return uploadedUrls;
                }

                // Split into chunks
                for (int i = 0; i < urls.size(); i += chunkSize) {
                        int end = Math.min(urls.size(), i + chunkSize);
                        List<UrlMeta> subList = urls.subList(i, end);
                        // 1-based index for file names: name-1.xml.gz, name-2.xml.gz ...
                        int partNumber = (i / chunkSize) + 1;
                        String fileName = fileBaseName + "-" + partNumber + ".xml.gz";
                        String xml = SitemapGenerator.generateSitemapXmlFromMeta(subList,
                                        commerceValueConfig.getSitemapXmlUrlPrefix());
                        byte[] compressed = compress(xml);
                        gcsService.uploadFile(commerceValueConfig.getSeoBucketName(), fileName, compressed,
                                        "application/xml",
                                        "gzip");
                        uploadedUrls.add(commerceValueConfig.getSitemapBaseUrl() + fileName);
                }

                return uploadedUrls;
        }

        private String uploadListToGcs(List<UrlMeta> urls, String fileName) {
                String xml = SitemapGenerator.generateSitemapXmlFromMeta(urls,
                                commerceValueConfig.getSitemapXmlUrlPrefix());
                byte[] compressed = compress(xml);
                gcsService.uploadFile(commerceValueConfig.getSeoBucketName(), fileName, compressed, "application/xml",
                                "gzip");
                return commerceValueConfig.getSitemapBaseUrl() + fileName;
        }

        private byte[] compress(String content) {
                try (java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                                java.util.zip.GZIPOutputStream gzip = new java.util.zip.GZIPOutputStream(bos)) {
                        gzip.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        gzip.finish();
                        return bos.toByteArray();
                } catch (Exception e) {
                        throw new GcsServiceException("Compression failed", e);
                }
        }

        private CategoryResponse processCategory(String categoryId, String categorySlug, String categoryType) {

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

                // For ALL_PRODUCTS: process products and variants (deep traversal)
                if (SeoConstants.CATEGORY_TYPE_ALL_PRODUCTS.equalsIgnoreCase(categoryType)) {
                        List<ProductResponse> products = processProducts(categoryId);

                        Set<String> cachedLocations = getLocationsFromCache(categoryId);
                        log.debug("Retrieved {} locations from cache for category: {}", cachedLocations.size(),
                                        categoryId);

                        UrlGroup categoryUrls = buildCategoryUrls(categoryContext, handler, cachedLocations);

                        clearCategoryLocationCache(categoryId);

                        return new CategoryResponse(
                                        categoryId,
                                        categoryType,
                                        categorySlug,
                                        categoryUrls,
                                        products);
                }

                // For all other categories (Brands, Industry Segments, etc.):
                // We still need to process products to extract locations for the category URLs,
                // but we DO NOT include the products in the Sitemap response.
                processProducts(categoryId);

                Set<String> cachedLocations = getLocationsFromCache(categoryId);
                log.debug("Retrieved {} locations from cache for category: {}", cachedLocations.size(),
                                categoryId);

                UrlGroup categoryUrls = buildCategoryUrls(categoryContext, handler, cachedLocations);

                clearCategoryLocationCache(categoryId);

                return new CategoryResponse(
                                categoryId,
                                categoryType,
                                categorySlug,
                                categoryUrls,
                                List.of()); // Empty description for non-all_products categories
        }

        private List<ProductResponse> processProducts(String categoryId) {

                log.debug("Fetching products for category: {}", categoryId);
                List<Product> productsFromCC = centralCatalogueClient.getAllProductsForCategoryId(categoryId,
                                SeoConstants.STOREFRONT_MSME);

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

                Map<String, ProductTypeData> productTypeMap = fetchProductTypes(productTypeIds);

                List<CompletableFuture<ProductResponse>> futures = productsFromCC.stream()
                                .map(product -> CompletableFuture.supplyAsync(() -> {
                                        try {
                                                return processProduct(product, categoryId, productTypeMap);
                                        } catch (Exception e) {
                                                log.error("Error processing product {}: {}", product.getId(),
                                                                e.getMessage(), e);
                                                return null;
                                        }
                                }, executorService))
                                .toList();

                List<ProductResponse> productResponses = futures.stream()
                                .map(future -> {
                                        try {
                                                return future.join();
                                        } catch (Exception e) {
                                                log.error("Error collecting product response: {}", e.getMessage());
                                                return null;
                                        }
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());

                log.info("Completed processing {} products for category: {}", productResponses.size(), categoryId);
                return productResponses;
        }

        /**
         * Process a single product to generate all URLs (product + variants)
         */
        private ProductResponse processProduct(Product product, String categoryId,
                        Map<String, ProductTypeData> productTypeMap) {
                if (product.getAttributes() == null || product.getAttributes().get(SeoConstants.ATTR_SLUG) == null) {
                        log.warn("Skipping product {} - missing slug attribute", product.getId());
                        return null;
                }
                String productBaseSlug = CatalogueUtil.str(product.getAttributes().get(SeoConstants.ATTR_SLUG));
                Instant productLastMod = parseLastModified(product.getLastModifiedAt());

                // Build base product context
                SeoContext productBaseContext = SeoContext.builder()
                                .entityType(SeoEntityType.PRODUCT)
                                .pageType(SeoPageType.PDP)
                                .categoryType(SeoConstants.CATEGORY_TYPE_ALL_PRODUCTS)
                                .productId(product.getId())
                                .slug(productBaseSlug)
                                .lastModifiedAt(productLastMod)
                                .operationType(SeoOperationType.URL_GENERATION)
                                .build();

                SeoPatternHandler handler = patternFactory.resolve(productBaseContext);

                // Base product URL
                UrlMeta baseUrl = handler.generateUrl(productBaseContext);

                // Build location-specific URLs
                Map<String, UrlMeta> stateUrls = buildProductLocationUrls(
                                product, productBaseSlug, productLastMod, categoryId, handler,
                                extractProductStates(product));

                Map<String, UrlMeta> cityUrls = buildProductLocationUrls(
                                product, productBaseSlug, productLastMod, categoryId, handler,
                                extractProductCities(product));

                // Combine for backwards compatibility or just use base in UrlGroup
                Map<String, UrlMeta> allLocationUrls = new HashMap<>();
                allLocationUrls.putAll(stateUrls);
                allLocationUrls.putAll(cityUrls);

                UrlGroup productUrls = new UrlGroup(baseUrl, allLocationUrls);

                // Build variant URLs
                List<VariantResponse> variants = new ArrayList<>();

                if (product.getProductTypeId() == null || product.getProductTypeId().isBlank()) {
                        log.error("Skipping variant URL generation for product {} - missing product type ID",
                                        product.getId());
                } else if (product.getVariants() != null && !product.getVariants().isEmpty()) {
                        ProductTypeData productTypeData = productTypeMap.get(product.getProductTypeId());

                        if (productTypeData == null) {
                                log.error("Product type data not found. typeId: {}, productId: {}",
                                                product.getProductTypeId(),
                                                product.getId());
                        }

                        List<String> productLocations = extractProductLocations(product);
                        variants = buildVariantUrls(product, productLocations, productLastMod, productTypeData,
                                        handler);
                }

                return new ProductResponse(
                                product.getId(),
                                productBaseSlug,
                                productUrls,
                                stateUrls,
                                cityUrls,
                                variants);
        }

        private Map<String, ProductTypeData> fetchProductTypes(Set<String> productTypeIds) {
                if (productTypeIds == null || productTypeIds.isEmpty()) {
                        return new HashMap<>();
                }

                // Uses ProductTypeService which handles caching internally
                return productTypeService.getProductTypes(productTypeIds, SeoConstants.STOREFRONT_MSME);
        }

        private String buildVariantAttributeSlug(Product product, Variant variant, ProductTypeData productTypeData) {

                String baseSlug = CatalogueUtil.str(product.getAttributes().get(SeoConstants.ATTR_SLUG));

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
                        if (SeoConstants.LOCATION_ALL.equalsIgnoreCase(location)) {
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
        public SeoMeta resolveSeoMeta(SeoContext seoContext, SeoData seoData) {

                SeoPatternHandler handler = patternFactory.resolve(seoContext);

                return handler.generateMeta(seoContext, seoData);
        }

        private List<CategoryIdentifier> fetchAllCategoryIdsFromCC() {

                List<CatalogueCategoryTree> categoryTree = centralCatalogueClient.getCategoryTree();

                if (categoryTree == null || categoryTree.isEmpty()) {
                        return List.of();
                }

                List<CategoryIdentifier> result = new ArrayList<>();

                for (CatalogueCategoryTree root : categoryTree) {
                        String categoryType = root.getKey();

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
                        String categoryType,
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

        /**
         * Extract unique location list from product's location data
         */
        private List<String> extractProductLocations(Product product) {
                if (product.getProductLocation() == null) {
                        return List.of();
                }

                return product.getProductLocation()
                                .stream()
                                .flatMap(location -> Stream.of(
                                                location.getState(),
                                                location.getDistrict()))
                                .filter(loc -> loc != null && !loc.isBlank())
                                .filter(loc -> !SeoConstants.LOCATION_ALL.equalsIgnoreCase(loc)) // Skip 'all' location
                                .distinct()
                                .toList();
        }

        /**
         * Build location-specific URLs for a product and cache locations
         */
        private Set<String> extractProductStates(Product product) {
                if (product.getProductLocation() == null) {
                        return Set.of();
                }
                return product.getProductLocation().stream()
                                .map(ProductLocation::getState)
                                .filter(loc -> loc != null && !loc.isBlank())
                                .filter(loc -> !SeoConstants.LOCATION_ALL.equalsIgnoreCase(loc))
                                .collect(Collectors.toSet());
        }

        private Set<String> extractProductCities(Product product) {
                if (product.getProductLocation() == null) {
                        return Set.of();
                }
                return product.getProductLocation().stream()
                                .map(ProductLocation::getDistrict)
                                .filter(loc -> loc != null && !loc.isBlank())
                                .filter(loc -> !SeoConstants.LOCATION_ALL.equalsIgnoreCase(loc))
                                .collect(Collectors.toSet());
        }

        /**
         * Build location-specific URLs for a product and cache locations
         */
        private Map<String, UrlMeta> buildProductLocationUrls(
                        Product product,
                        String productBaseSlug,
                        Instant productLastMod,
                        String categoryId,
                        SeoPatternHandler handler,
                        Set<String> locations) {

                Map<String, UrlMeta> locationUrls = new ConcurrentHashMap<>();

                for (String location : locations) {
                        // Add to cache for category URL generation
                        addLocationToCache(categoryId, location);

                        SeoContext locationContext = SeoContext.builder()
                                        .entityType(SeoEntityType.PRODUCT)
                                        .pageType(SeoPageType.PDP)
                                        .categoryType(SeoConstants.CATEGORY_TYPE_ALL_PRODUCTS)
                                        .productId(product.getId())
                                        .slug(productBaseSlug)
                                        .location(location)
                                        .lastModifiedAt(productLastMod)
                                        .operationType(SeoOperationType.URL_GENERATION)
                                        .build();

                        UrlMeta locationUrl = handler.generateUrl(locationContext);
                        locationUrls.put(location, locationUrl);
                }

                return locationUrls;
        }

        /**
         * Build variant URLs for all variants of a product
         */
        private List<VariantResponse> buildVariantUrls(
                        Product product,
                        List<String> productLocations,
                        Instant productLastMod,
                        ProductTypeData productTypeData,
                        SeoPatternHandler handler) {

                List<VariantResponse> variants = new ArrayList<>();

                if (product.getVariants() == null || product.getVariants().isEmpty()) {
                        return variants;
                }

                for (Variant variant : product.getVariants()) {
                        Map<String, UrlMeta> variantLocationUrls = new ConcurrentHashMap<>();

                        for (String location : productLocations) {
                                String variantBaseSlug = buildVariantAttributeSlug(product, variant, productTypeData);

                                SeoContext variantContext = SeoContext.builder()
                                                .entityType(SeoEntityType.VARIANT)
                                                .pageType(SeoPageType.PDP)
                                                .categoryType(SeoConstants.CATEGORY_TYPE_ALL_PRODUCTS)
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
                                variants.add(new VariantResponse(
                                                variant.getVariantMmid(),
                                                new UrlGroup(null, variantLocationUrls)));
                        }
                }

                return variants;
        }
}
