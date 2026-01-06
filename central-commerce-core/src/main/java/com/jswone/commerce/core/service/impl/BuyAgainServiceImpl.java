package com.jswone.commerce.core.service.impl;

import com.commercetools.api.models.customer.Customer;
import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.constants.CacheNames;
import com.jswone.commerce.core.entity.PurchasedSku;
import com.jswone.commerce.core.entity.catalogue.*;
import com.jswone.commerce.core.model.BuyAgainResponse;
import com.jswone.commerce.core.model.PurchasedLineItemResponse;
import com.jswone.commerce.core.repository.ProductCatalogueStoreRepository;
import com.jswone.commerce.core.service.BuyAgainService;
import com.jswone.commerce.core.service.PurchasedSkuService;
import com.jswone.commerce.core.util.JSWCustomerUtil;
import com.jswone.commons.constants.JSWGenericConstants;
import com.jswone.commons.util.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.jswone.commerce.core.config.ProfileAwareCacheConfig.getCacheNameWithProfile;
import static com.jswone.commerce.core.constants.JSWProductConstants.EMPTY_STRING;
import static com.jswone.commerce.core.constants.JWTConstants.HYPHEN;

@Slf4j
@Service
public class BuyAgainServiceImpl implements BuyAgainService {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private final PurchasedSkuService purchasedSkuService;
    private final ProductCatalogueStoreRepository productCatalogueStoreRepository;
    private final JSWCustomerUtil customerUtil;
    private final CacheManager cacheManager;
    private final CommerceValueConfig commerceValueConfig;

    public BuyAgainServiceImpl(PurchasedSkuService purchasedSkuService, ProductCatalogueStoreRepository productCatalogueStoreRepository,
                               JSWCustomerUtil customerUtil, CacheManager cacheManager, CommerceValueConfig commerceValueConfig) {
        this.purchasedSkuService = purchasedSkuService;
        this.productCatalogueStoreRepository = productCatalogueStoreRepository;
        this.customerUtil = customerUtil;
        this.cacheManager = cacheManager;
        this.commerceValueConfig = commerceValueConfig;
    }

    @Override
    public BuyAgainResponse getRecentPurchasedDistributedOrdersList(int offset, int limit) {
        List<PurchasedLineItemResponse> variantList = getRecentPurchasedDistributedOrdersHome(offset, limit)
                .getVariantList();
        return buildDistributedBuyAgainResponse(variantList);
    }

    public BuyAgainResponse getRecentPurchasedDistributedOrdersHome(int offset, int limit) {

        String customerId = JwtTokenUtil.getUserIdForSession();

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
            BuyAgainResponse cachedBuyAgainResponse = (BuyAgainResponse) wrapper.get();
            if (cachedBuyAgainResponse != null && cachedBuyAgainResponse.getVariantList() != null) {
                return getPagedDistributedBuyAgainResponse(offset, limit, cachedBuyAgainResponse.getVariantList());
            }
            return buildDistributedBuyAgainResponse(Collections.emptyList());
        }

        // Cache miss
        log.info("Buy_Again - Cache MISS for customerId={}, fetching from DB", customerId);
        List<PurchasedSku> purchasedSkus = fetchAndSortPurchasedSkus(customerId);
        BuyAgainResponse buyAgainResponse = getRecentPurchasedDistributed(purchasedSkus);

        // Cache a defensive copy
        cache.put(customerId, defensivelyCopyBuyAgainResponse(buyAgainResponse)); // Cache the response
        log.info("Buy_Again - Cached Buy Again Response for customerId={}", customerId);

        return getPagedDistributedBuyAgainResponse(offset, limit, buyAgainResponse.getVariantList());
    }


    /**
     * Builds an immutable copy of the response so cached entries are not accidentally mutated by
     * callers.
     */
    public BuyAgainResponse defensivelyCopyBuyAgainResponse(BuyAgainResponse src) {
        if (src == null) return buildDistributedBuyAgainResponse(Collections.emptyList());
        List<PurchasedLineItemResponse> list = Optional.ofNullable(src.getVariantList())
                .map(ArrayList::new)
                .map(Collections::unmodifiableList)
                .orElse(Collections.emptyList());
        return BuyAgainResponse.builder()
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
                .sorted(Comparator.comparing(
                        PurchasedSku::getOrderPlacedDate,
                        Comparator.nullsLast(Date::compareTo)).reversed())
                .collect(Collectors.toList());
        log.info("Buy_Again - Fetched {} purchased skus for customerId={}", purchasedSkus.size(), customerId);
        return purchasedSkus;
    }

    /**
     * Main builder that resolves product store data and central catalogue products and then
     * constructs the final list of {@link PurchasedLineItemResponse}.
     */
    public BuyAgainResponse getRecentPurchasedDistributed(List<PurchasedSku> purchasedSkus) {
        if (purchasedSkus == null || purchasedSkus.isEmpty()) {
            return buildDistributedBuyAgainResponse(Collections.emptyList());
        }

        Set<String> productMMIDList = getProductMMIDList(purchasedSkus);

        log.info("Buy_Again - Fetching Product Catalogue Store");
        List<ProductCatalogueStore> productCatalogueStoreList = getProductDataStoreInBatches(productMMIDList);
        log.info("Buy_Again - Fetched Product Catalogue Store for ProductMMIDCount={}", productMMIDList.size());
        Map<String, ProductCatalogueStore> productCatalogueStoreMap = productCatalogueStoreList.stream()
                .collect(Collectors.toMap(ProductCatalogueStore::getProductMaterialMasterId, Function.identity()));
        List<PurchasedLineItemResponse> lineItemResponseList = new ArrayList<>();
        log.info("Buy_Again - Preparing purchased line item response");

        for (PurchasedSku purchasedSku : purchasedSkus) {
            String productMMID = resolveProductMMID(purchasedSku);
            if (StringUtils.isEmpty(productMMID)) {
                log.warn("Buy_Again - Skipping Purchased SKU as Product MMID is missing");
                continue;
            }
            log.info("Buy_Again - Preparing purchased line item response for Purchased SKU={} ", purchasedSku);
            ProductCatalogueStore productCatalogueStore = productCatalogueStoreMap.get(productMMID);

            if (Objects.nonNull(productCatalogueStore)) {
                lineItemResponseList.add(buildPurchasedLineItemResponse(purchasedSku, productCatalogueStore));
                log.info("Buy_Again - Prepared purchased line item response list of size={}", lineItemResponseList.size());
            }
        }
        return buildDistributedBuyAgainResponse(lineItemResponseList);
    }

    private PurchasedLineItemResponse buildPurchasedLineItemResponse(PurchasedSku purchasedSku, ProductCatalogueStore productCatalogueStore) {
        com.jswone.commerce.core.entity.catalogue.Variant matchVariant = validateVariant(purchasedSku.getVariantKey(), productCatalogueStore);

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        String orderPlacedDate = purchasedSku.getOrderPlacedDate() == null ? EMPTY_STRING : sdf.format(purchasedSku.getOrderPlacedDate());

        return PurchasedLineItemResponse.builder()
                .name(productCatalogueStore.getProductTitle())
                .productKey(productCatalogueStore.getProductKey())
                .productSlug(productCatalogueStore.getProductSlug())
                .attributes(
                        matchVariant != null
                                && !matchVariant
                                .getAttributes()
                                .isEmpty()
                                ? transformPurchaseSkuAttributes(
                                matchVariant.getAttributes(),
                                purchasedSku.getSkuAttributes())
                                : purchasedSku.getSkuAttributes())
                .ctAttributes(purchasedSku.getCtSkuAttributes())
                .variantKey(purchasedSku.getVariantKey())
                .quantityCard(productCatalogueStore
                        .getPdpJourney() == null || productCatalogueStore
                        .getPdpJourney()
                        .getQuantityCard() == null ? QuantityCard.builder().build() : productCatalogueStore
                        .getPdpJourney()
                        .getQuantityCard())
                .attributesMeta(purchasedSku.getCtSkuAttributes().keySet())
                .sku(purchasedSku.getVariantName())
                .primaryUom(purchasedSku.getPrimaryQuantity())
                .secondaryUom(purchasedSku.getSecondaryQuantity())
                .ctUom(purchasedSku.getCtUom())
                .orderPlacedDate(orderPlacedDate)
                .productMMID(productCatalogueStore.getProductMaterialMasterId())
                .productTypeKey(productCatalogueStore.getProductTypeKey())
                .variantMMID(
                        StringUtils.isEmpty(
                                productCatalogueStore
                                        .getProductMaterialMasterId())
                                ? JSWGenericConstants.EMPTY_STRING
                                : productCatalogueStore
                                .getProductMaterialMasterId()
                                .concat(HYPHEN)
                                .concat("10000000"))
                .imageUrl(
                        Optional.of(productCatalogueStore)
                                .map(ProductCatalogueStore::getProductMedia)
                                .map(ProductMedia::getImages)
                                .filter(list -> !list.isEmpty())
                                .map(list -> list.get(0))
                                .map(Image::getUrl)
                                .orElse(EMPTY_STRING))
                .build();
    }

    private BuyAgainResponse buildDistributedBuyAgainResponse(List<PurchasedLineItemResponse> variantList) {
        List<PurchasedLineItemResponse> purchasedLineItemResponseList = variantList == null ? Collections.emptyList() : List.copyOf(variantList);
        return BuyAgainResponse.builder()
                .skuSize(purchasedLineItemResponseList.size())
                .variantList(purchasedLineItemResponseList)
                .build();
    }

    private BuyAgainResponse getPagedDistributedBuyAgainResponse(int offset, int limit,
                                                                 List<PurchasedLineItemResponse> purchasedLineItems) {
        if (purchasedLineItems == null || purchasedLineItems.isEmpty())
            return buildDistributedBuyAgainResponse(Collections.emptyList());

        int start = Math.min(offset, purchasedLineItems.size());
        int end = Math.min(start + limit, purchasedLineItems.size());

        List<PurchasedLineItemResponse> pagedPurchasedLineItems = purchasedLineItems.subList(start, end);

        return buildDistributedBuyAgainResponse(pagedPurchasedLineItems);
    }

    private Set<String> getProductMMIDList(List<PurchasedSku> purchasedSkus) {
        return purchasedSkus.stream()
                .map(this::resolveProductMMID)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
    }

    private String resolveProductMMID(PurchasedSku purchasedSku) {
        if (purchasedSku == null) return null;
        String productMMID = purchasedSku.getProductMMID();
        return StringUtils.isEmpty(productMMID) ? null : productMMID;
    }

    private Map<String, String> transformPurchaseSkuAttributes(
            List<Attribute> attributeList, Map<String, String> ctSkuAttributes) {
        return attributeList.stream()
                .collect(
                        Collectors.toMap(
                                attribute -> getAttributeName(
                                        attribute.getName(), ctSkuAttributes),
                                attribute ->
                                        getAttributeValue(
                                                attribute.getValue(),
                                                attribute.getName(),
                                                ctSkuAttributes),
                                (e1, e2) -> e2,
                                LinkedHashMap::new));
    }

    public String getAttributeName(
            String ctAttributeName, Map<String, String> ctSkuAttributes) {
        Map.Entry<String, String> values = getAttributeMap(ctAttributeName, ctSkuAttributes);
        return values != null ? values.getKey() : ctAttributeName;
    }

    public String getAttributeValue(
            Object mainAttributeValue,
            String ctAttributeName,
            Map<String, String> ctSkuAttributes) {
        Map.Entry<String, String> values = getAttributeMap(ctAttributeName, ctSkuAttributes);
        return values != null ? values.getValue() : String.valueOf(mainAttributeValue);
    }

    public Map.Entry<String, String> getAttributeMap(
            String ctAttributeName, Map<String, String> ctSkuAttributes) {
        return ctSkuAttributes.entrySet().stream()
                .filter(
                        stringStringEntry -> {
                            String[] splitAttrVal = stringStringEntry.getKey().split(" ");
                            return Arrays.stream(splitAttrVal)
                                    .anyMatch(
                                            key ->
                                                    ctAttributeName
                                                            .toLowerCase()
                                                            .contains(key.toLowerCase()));
                        })
                .findFirst()
                .orElse(null);
    }

    public Variant validateVariant(String variantKey, ProductCatalogueStore productCatalogueStore) {

        if (CollectionUtils.isEmpty(productCatalogueStore.getVariants())
                && !productCatalogueStore.getHasVariant()
                && Objects.nonNull(productCatalogueStore.getMasterVariant())) {
            return variantKey.equalsIgnoreCase(
                    productCatalogueStore.getMasterVariant().getVariantKey())
                    ? productCatalogueStore.getMasterVariant()
                    : null;
        }

        return productCatalogueStore.getVariants().stream()
                .filter(variant -> variant.getVariantKey().equals(variantKey))
                .findAny()
                .orElse(null);
    }

    public List<ProductCatalogueStore> getProductDataStoreInBatches(Set<String> productMMIds) {
        int batchSize = 30;
        List<ProductCatalogueStore> productStoreList = new ArrayList<>();
        List<String> productMMIDList = new ArrayList<>(productMMIds);
        for (int i = 0; i < productMMIDList.size(); i += batchSize) {
            List<String> batch =
                    productMMIDList.subList(i, Math.min(i + batchSize, productMMIDList.size()));
            List<ProductCatalogueStore> batchResults =
                    productCatalogueStoreRepository.findProductCatalogueStoresByProductMaterialMasterIds(batch);

            productStoreList.addAll(batchResults);
        }
        return productStoreList;
    }

    public Cache getBuyAgainCache() {
        Cache cache = cacheManager.getCache(getCacheNameWithProfile(commerceValueConfig.getRedisCacheProfile(), CacheNames.BUY_AGAIN_PRODUCTS_CACHE_PREFIX));
        if (cache == null) {
            log.error("Buy_Again - Cache '{}' not found in CacheConfig", CacheNames.BUY_AGAIN_PRODUCTS_CACHE_PREFIX);
            throw new IllegalStateException("Cache not configured: " + CacheNames.BUY_AGAIN_PRODUCTS_CACHE_PREFIX);
        }
        return cache;
    }
}
