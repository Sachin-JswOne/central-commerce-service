package com.jswone.commerce.core.service.impl;

import com.commercetools.api.models.customer.Customer;
import com.jswone.commerce.core.constants.CacheNames;
import com.jswone.commerce.core.entity.PurchasedSku;
import com.jswone.commerce.core.entity.catalogue.ProductCatalogueStore;
import com.jswone.commerce.core.entity.catalogue.ProductMedia;
import com.jswone.commerce.core.entity.catalogue.QuantityCard;
import com.jswone.commerce.core.model.DistributedBuyAgainResponse;
import com.jswone.commerce.core.model.PurchasedLineItemResponse;
import com.jswone.commerce.core.model.Uom;
import com.jswone.commerce.core.model.centralCatalogue.Product;
import com.jswone.commerce.core.model.centralCatalogue.Variant;
import com.jswone.commerce.core.model.request.ProductBulkRequest;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductBulkResponse;
import com.jswone.commerce.core.repository.ProductCatalogueStoreRepository;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.service.BuyAgainService;
import com.jswone.commerce.core.service.PurchasedSkuService;
import com.jswone.commerce.core.util.JSWCustomerUtil;
import com.jswone.commons.util.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.jswone.commerce.core.config.ProfileAwareCacheConfig.getCacheNameWithProfile;
import static com.jswone.commerce.core.constants.BuyAgainConstants.LOCALE_EN_US;
import static com.jswone.commerce.core.constants.JSWProductConstants.EMPTY_STRING;
import static com.jswone.commerce.core.constants.JWTConstants.HYPHEN;
import static com.jswone.commerce.core.util.CatalogueUtil.str;

@Slf4j
@Service
public class BuyAgainServiceImpl implements BuyAgainService {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private final PurchasedSkuService purchasedSkuService;
    private final ProductCatalogueStoreRepository productCatalogueStoreRepository;
    private final CentralCatalogueClient centralCatalogueClient;
    private final JSWCustomerUtil customerUtil;
    private final CacheManager cacheManager;
    @Value("${pdp.journey.enabled}")
    private boolean PDP_JOURNEY_ENABLED;

    @Value("${buyagain.warmup.thread-count:4}")
    private int warmupThreadCount;

    @Value("${buyagain.warmup.max-entries:10000}")
    private long warmupMaxEntries;

    @Value("${redis.profile}")
    private String cacheProfile;

    public BuyAgainServiceImpl(PurchasedSkuService purchasedSkuService, ProductCatalogueStoreRepository productCatalogueStoreRepository,
                               CentralCatalogueClient centralCatalogueClient, JSWCustomerUtil customerUtil, CacheManager cacheManager) {
        this.purchasedSkuService = purchasedSkuService;
        this.productCatalogueStoreRepository = productCatalogueStoreRepository;
        this.centralCatalogueClient = centralCatalogueClient;
        this.customerUtil = customerUtil;
        this.cacheManager = cacheManager;
    }

    /**
     * Returns a paged {@link DistributedBuyAgainResponse} for the current (session) user.
     * This method enforces pagination bounds, reads from cache when possible and falls
     * back to the database and central catalogue when the cache misses.
     *
     * @param offset zero-based start index
     * @param limit  maximum number of items to return
     * @return paged {@link DistributedBuyAgainResponse};
     */
    @Override
    public DistributedBuyAgainResponse getRecentPurchasedDistributedOrdersList(int offset, int limit) {
        List<PurchasedLineItemResponse> variantList = getRecentPurchasedDistributedOrdersHome(offset, limit)
                .getVariantList();
        return buildDistributedBuyAgainResponse(variantList);
    }

    //    /**
//     * Bulk load buy again products for all customers into Redis.
//     */
//    public void loadAllBuyAgainProductsForCustomersIntoCache() {
//        log.info("Starting Redis warm-up for buy again products for all customers...");
//        List<PurchasedSku> purchasedSkuForAllCustomers = purchasedSkuService.fetchRecentlyPurchasedSkuForAllCustomers();
//
//        Map<String, List<PurchasedSku>> groupedByCustomer =
//                purchasedSkuForAllCustomers.stream().collect(Collectors.groupingBy(PurchasedSku::getCustomerId));
//
//        Map<String, DistributedBuyAgainResponse> groupedDistributedBuyAgainResponseByCustomer =
//                groupedByCustomer.entrySet().stream()
//                        .collect(Collectors.toMap(Map.Entry::getKey, entry ->
//                                getRecentPurchasedDistributed(entry.getValue() == null ? List.of() : entry.getValue())));
//
//        Cache cache = getCache();
//        groupedDistributedBuyAgainResponseByCustomer.forEach(cache::put);
//
//        log.info("Cached Buy Again Products For All Customers: {}", groupedByCustomer.size());
//    }

    /**
     * Bulk warm-up: load buy-again products for customers into cache.
     *
     * <p>Important: the underlying data source API may return a very large result set. To avoid
     * OOM and to reduce pressure on Redis, we guard with {@code warmupMaxEntries}. If the fetched
     * number of purchased SKU entries exceeds that threshold we switch to a conservative path: log and
     * skip bulk caching (explicitly forcing operator attention).
     */
    public void loadAllBuyAgainProductsForCustomersIntoCache() {

        log.info("BUY_AGAIN — Cache warm-up started...");

        // Fetch all purchase data
        List<PurchasedSku> purchasedSkuForAllCustomers =
                purchasedSkuService.fetchRecentlyPurchasedSkuForAllCustomers();

        if (purchasedSkuForAllCustomers == null || purchasedSkuForAllCustomers.isEmpty()) {
            log.info("BUY_AGAIN — Warm-up skipped: No purchase data found");
            return;
        }

        // Safety cap to avoid memory/Redis overload
        if (purchasedSkuForAllCustomers.size() > warmupMaxEntries) {
            log.warn("BUY_AGAIN — Warm-up aborted: {} entries exceed max limit ({})",
                    purchasedSkuForAllCustomers.size(), warmupMaxEntries);
            return;
        }

        // Group by customer ID
        Map<String, List<PurchasedSku>> grouped =
                purchasedSkuForAllCustomers.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.groupingBy(PurchasedSku::getCustomerId));

        Cache cache = getBuyAgainCache();
        int success = 0;
        int failed = 0;

        // Process each customer sequentially
        for (Map.Entry<String, List<PurchasedSku>> entry : grouped.entrySet()) {
            String customerId = entry.getKey();
            List<PurchasedSku> skus = entry.getValue();

            try {
                // Build response for customer
                DistributedBuyAgainResponse resp =
                        getRecentPurchasedDistributed(skus == null ? List.of() : skus);

                // Defensive copy of response for safety
                DistributedBuyAgainResponse safeCopy = defensivelyCopyResponse(resp);

                // Put into cache
                cache.put(customerId, safeCopy);

                success++;
            } catch (Exception e) {
                failed++;
                log.error("BUY_AGAIN — Warm-up failed for customerId={}", customerId, e);
            }
        }

        log.info("BUY_AGAIN — Warm-up completed. Total customers={}, Success={}, Failed={}",
                grouped.size(), success, failed);
    }

    /**
     * Main entry used by controller flows: checks cache and falls back to DB + remote calls on miss.
     * This method returns a paged response.
     */

    public DistributedBuyAgainResponse getRecentPurchasedDistributedOrdersHome(int offset, int limit) {

        String customerId = JwtTokenUtil.getUserIdForSession();

//        String customerId = "6602c25d-d2d5-4d67-9166-0452d09e6994";

        log.info("Buy_Again - Fetching customer for customerId={}", customerId);

        // Fetch customer data
        Customer customer = customerUtil.getCustomerById(customerId);
        if (Objects.isNull(customer)) {
            log.error("Buy_Again - Customer not found for customerId={}", customerId);
            return buildDistributedBuyAgainResponse(Collections.emptyList());
        }

        // Check cache
        Cache cache = getBuyAgainCache();
        Cache.ValueWrapper wrapper = cache.get(customerId);

        if (wrapper != null) {
            log.info("Buy_Again - Cache HIT for customerId={}", customerId);
            DistributedBuyAgainResponse cachedDistributedBuyAgainResponse = (DistributedBuyAgainResponse) wrapper.get();
            if (cachedDistributedBuyAgainResponse != null && cachedDistributedBuyAgainResponse.getVariantList() != null) {
                return getPagedDistributedBuyAgainResponse(offset, limit, cachedDistributedBuyAgainResponse.getVariantList());
            }
            return buildDistributedBuyAgainResponse(Collections.emptyList());
        }

        // Cache miss
        log.info("Buy_Again - Cache MISS for customerId={}, fetching from DB", customerId);
        List<PurchasedSku> purchasedSkus = fetchAndSortPurchasedSkus(customerId);
        DistributedBuyAgainResponse distributedBuyAgainResponse = getRecentPurchasedDistributed(purchasedSkus);

        // Cache a defensive copy
        cache.put(customerId, defensivelyCopyResponse(distributedBuyAgainResponse)); // Cache the response
        log.info("Buy_Again - Cached Buy Again Response for customerId={}", customerId);

        return getPagedDistributedBuyAgainResponse(offset, limit, distributedBuyAgainResponse.getVariantList());
    }


    /**
     * Builds an immutable copy of the response so cached entries are not accidentally mutated by
     * callers.
     */
    private DistributedBuyAgainResponse defensivelyCopyResponse(DistributedBuyAgainResponse src) {
        if (src == null) return buildDistributedBuyAgainResponse(Collections.emptyList());
        List<PurchasedLineItemResponse> list = Optional.ofNullable(src.getVariantList())
                .map(ArrayList::new)
                .map(Collections::unmodifiableList)
                .orElse(Collections.emptyList());
        return DistributedBuyAgainResponse.builder()
                .skuSize(list.size())
                .variantList(list)
                .build();
    }

    /**
     * Null-safe fetch + sort purchased SKUs by order placed date.
     */
    private List<PurchasedSku> fetchAndSortPurchasedSkus(String customerId) {
        log.info("Buy_Again - Fetching purchased SKU for customerId={} ", customerId);
        List<PurchasedSku> purchasedSkus = Optional.ofNullable(purchasedSkuService.fetchRecentlyPurchasedSku(customerId))
                .orElseGet(Collections::emptyList)
                .stream()
                .sorted(Comparator.comparing(PurchasedSku::getOrderPlacedDate).reversed())
                .collect(Collectors.toList());
        log.info("Buy_Again - Fetched {} purchased skus for customerId={}", purchasedSkus.size(), customerId);
        return purchasedSkus;
    }

    /**
     * Main builder that resolves product store data and central catalogue products and then
     * constructs the final list of {@link PurchasedLineItemResponse}.
     */
    public DistributedBuyAgainResponse getRecentPurchasedDistributed(List<PurchasedSku> purchasedSkus) {
        if (purchasedSkus == null || purchasedSkus.isEmpty()) {
            return buildDistributedBuyAgainResponse(Collections.emptyList());
        }

        Set<String> productKeys = purchasedSkus.stream()
                .map(PurchasedSku::getProductKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        log.info("Buy_Again - Fetching Product Catalogue Store");
        List<ProductCatalogueStore> productCatalogueStoreList = getProductDataStoreInBatches(productKeys);
        log.info("Buy_Again - Fetched Product Catalogue Store for ProductKeyCount={}", productKeys.size());
        Map<String, ProductCatalogueStore> productCatalogueStoreMap = productCatalogueStoreList.stream()
                .collect(Collectors.toMap(ProductCatalogueStore::getProductKey, Function.identity()));

        Set<String> productMMIDList = getProductMMIDList(purchasedSkus, productCatalogueStoreMap);
        log.info("Buy_Again - Calling Cental Catalogue Product Bulk API with ProductMMIDCount={}", productMMIDList.size());
        ProductBulkResponse productBulkMMIDResponse = fetchCentralCatalogueProductsWithRetry(productMMIDList);
        Map<String, Product> centralCatalogueProductMap = mapCentralCatalogueProducts(productBulkMMIDResponse);

        List<PurchasedLineItemResponse> lineItemResponseList = new ArrayList<>();
        log.info("Buy_Again - Preparing purchased line item response");

        for (PurchasedSku purchasedSku : purchasedSkus) {
            log.info("Buy_Again - Preparing purchased line item response for Purchased SKU={} ", purchasedSku);
            ProductCatalogueStore productCatalogueStore = productCatalogueStoreMap.get(purchasedSku.getProductKey());
            String productMMID = resolveProductMMID(purchasedSku, productCatalogueStore);

            Product centralCatalogueProduct = centralCatalogueProductMap.get(productMMID);

            if (Objects.nonNull(centralCatalogueProduct)) {

                lineItemResponseList.add(buildPurchasedLineItemResponse(purchasedSku, centralCatalogueProduct, productCatalogueStore));
                log.info("Buy_Again - Prepared purchased line item response list of size={}", lineItemResponseList.size());
            }
        }
        return buildDistributedBuyAgainResponse(lineItemResponseList);
    }

    private PurchasedLineItemResponse buildPurchasedLineItemResponse(PurchasedSku purchasedSku, Product centralProduct, ProductCatalogueStore productStore) {
        Variant matchVariant = validateVariant(purchasedSku.getVariantKey(), centralProduct);

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        String orderPlacedDate = purchasedSku.getOrderPlacedDate() == null ? EMPTY_STRING : sdf.format(purchasedSku.getOrderPlacedDate());

        return PurchasedLineItemResponse.builder()
                .name(str(centralProduct.getAttributes().get("product_title")))
                .productSlug(str(centralProduct.getAttributes().get("slug")))
                .attributes(Optional.ofNullable(matchVariant)
                        .map(Variant::getAttributes)
                        .orElse(purchasedSku.getSkuAttributes()))
                .ctAttributes(purchasedSku.getCtSkuAttributes())
                .variantKey(purchasedSku.getVariantKey())
                .quantityCard(resolveQuantityCard(productStore))
                .attributesMeta(purchasedSku.getCtSkuAttributes() == null ? Collections.emptySet() : purchasedSku.getCtSkuAttributes().keySet())
                .sku(purchasedSku.getVariantName())
                .primaryUom(purchasedSku.getPrimaryQuantity())
                .secondaryUom(generateSecondaryUom(purchasedSku))
                .ctUom(purchasedSku.getCtUom())
                .orderPlacedDate(orderPlacedDate)
                .productMMID(centralProduct.getProductMmid())
                .productTypeKey(productStore.getProductTypeKey())
                .variantMMID(generateVariantMMID(centralProduct))
                .imageUrl(resolveImageUrl(productStore))
                .build();
    }

    private DistributedBuyAgainResponse buildDistributedBuyAgainResponse(List<PurchasedLineItemResponse> variantList) {
        List<PurchasedLineItemResponse> purchasedLineItemResponseList = variantList == null ? Collections.emptyList() : List.copyOf(variantList);
        return DistributedBuyAgainResponse.builder()
                .skuSize(purchasedLineItemResponseList.size())
                .variantList(purchasedLineItemResponseList)
                .build();
    }

    private DistributedBuyAgainResponse getPagedDistributedBuyAgainResponse(int offset, int limit,
                                                                            List<PurchasedLineItemResponse> purchasedLineItems) {
        if (purchasedLineItems == null || purchasedLineItems.isEmpty())
            return buildDistributedBuyAgainResponse(Collections.emptyList());

        int start = Math.min(offset, purchasedLineItems.size());
        int end = Math.min(start + limit, purchasedLineItems.size());

        List<PurchasedLineItemResponse> pagedPurchasedLineItems = purchasedLineItems.subList(start, end);

        return buildDistributedBuyAgainResponse(pagedPurchasedLineItems);
    }

    private Set<String> getProductMMIDList(List<PurchasedSku> purchasedSkus, Map<String, ProductCatalogueStore> productCatalogueStoreMap) {
        return purchasedSkus.stream()
                .map(purchasedSku ->
                        resolveProductMMID(purchasedSku, productCatalogueStoreMap.get(purchasedSku.getProductKey())))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private ProductBulkResponse fetchCentralCatalogueProductsWithRetry(Set<String> productMMIDList) {

        if (productMMIDList == null || productMMIDList.isEmpty()) {
            return new ProductBulkResponse(Collections.emptyList(), 0);
        }

        log.info("Buy_Again - Calling Central Catalogue Product Bulk API with ProductMMID Count={}",
                productMMIDList.size());

        try {
            ProductBulkRequest request =
                    new ProductBulkRequest(productMMIDList, "msme", LOCALE_EN_US);
            return centralCatalogueClient.bulkMMIDResponse(request);
        } catch (Exception e) {
            log.error("Buy_Again - Central catalogue call failed (client retries already attempted): {}",
                    e.getMessage(), e);
            return new ProductBulkResponse(Collections.emptyList(), 0);
        }
    }

    private QuantityCard resolveQuantityCard(ProductCatalogueStore productCatalogueStore) {
        return PDP_JOURNEY_ENABLED
                ? productCatalogueStore.getPdpJourney().getQuantityCard()
                : productCatalogueStore.getDistributedJourney().getQuantityCard();
    }

    private String resolveImageUrl(ProductCatalogueStore productCatalogueStore) {
        return Optional.ofNullable(productCatalogueStore)
                .map(ProductCatalogueStore::getProductMedia)
                .map(ProductMedia::getImages)
                .filter(images -> !images.isEmpty())
                .map(images -> images.getFirst().getUrl())
                .orElse(EMPTY_STRING);
    }

    private String generateVariantMMID(Product centralCatalogueProduct) {
        if (centralCatalogueProduct == null) {
            return EMPTY_STRING;
        }
        return StringUtils.isEmpty(centralCatalogueProduct.getProductMmid())
                ? EMPTY_STRING
                : centralCatalogueProduct.getProductMmid() + HYPHEN + "10000000";
    }

    private String resolveProductMMID(PurchasedSku purchasedSku, ProductCatalogueStore productCatalogueStore) {
        if (purchasedSku == null) return null;
        if (!StringUtils.isEmpty(purchasedSku.getProductMMID())) {
            return purchasedSku.getProductMMID();
        }
        return productCatalogueStore == null ? null : productCatalogueStore.getProductMaterialMasterId();
    }

    public Variant validateVariant(String variantKey, Product centralCatalogueProduct) {
        if (centralCatalogueProduct == null || centralCatalogueProduct.getVariants() == null)
            return null;
        return centralCatalogueProduct.getVariants().stream()
                .filter(variant -> variantKey != null && variantKey.equals(variant.getVariantMmid()))
                .findAny()
                .orElse(null);
    }

    private Uom generateSecondaryUom(PurchasedSku purchasedSku) {
        if (purchasedSku == null || purchasedSku.getSecondaryQuantity() == null) return null;
        Uom uom = purchasedSku.getSecondaryQuantity();
        int value = Optional.of(uom.getValue()).map(Double::intValue).orElse(0);
        return Uom.builder()
                .unit(uom.getUnit())
                .label(uom.getLabel())
                .priceLabel(uom.getPriceLabel())
                .value(value)
                .build();
    }


    public List<ProductCatalogueStore> getProductDataStoreInBatches(Set<String> productKeys) {
        int batchSize = 30;
        List<ProductCatalogueStore> productStoreList = new ArrayList<>();
        List<String> productKeyString = new ArrayList<>(productKeys);
        for (int i = 0; i < productKeyString.size(); i += batchSize) {
            List<String> batch =
                    productKeyString.subList(i, Math.min(i + batchSize, productKeyString.size()));
            List<ProductCatalogueStore> batchResults =
                    productCatalogueStoreRepository.findProductCatalogueStoresByProductKeys(batch);

            productStoreList.addAll(batchResults);
        }
        return productStoreList;
    }

    private Map<String, Product> mapCentralCatalogueProducts(ProductBulkResponse productBulkResponse) {
        return productBulkResponse != null
                ? productBulkResponse.getProducts().stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(Product::getProductMmid, Function.identity(), (a, b) -> a))
                : Collections.emptyMap();
    }

    private Cache getBuyAgainCache() {
        Cache cache = cacheManager.getCache(getCacheNameWithProfile(cacheProfile, CacheNames.BUY_AGAIN_PRODUCTS));
        if (cache == null) {
            log.error("Buy_Again - Cache '{}' not found in CacheConfig", CacheNames.BUY_AGAIN_PRODUCTS);
            throw new IllegalStateException("Cache not configured: " + CacheNames.BUY_AGAIN_PRODUCTS);
        }
        return cache;
    }
}
