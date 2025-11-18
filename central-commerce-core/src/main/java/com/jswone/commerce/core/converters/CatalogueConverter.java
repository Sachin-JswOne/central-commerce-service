package com.jswone.commerce.core.converters;

import com.jswone.commerce.core.config.CatalogueDynamicConfig;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.centralCatalogue.Product;
import com.jswone.commerce.core.model.request.Search.SearchRequest;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductSearchResponse;
import com.jswone.commerce.core.model.response.plp.PLPAttribute;
import com.jswone.commerce.core.model.response.plp.PLPCard;
import com.jswone.commerce.core.model.response.plp.ProductFilterConditions;
import com.jswone.commerce.core.model.response.search.SearchResponse;
import com.jswone.commerce.core.model.response.search.SearchSuggestion;

import com.jswone.commerce.core.util.CatalogueUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CatalogueConverter {

    private final Set<String> NON_ATTRIBUTE_KEYS;
    private final Set<String> ATTRIBUTE_KEYS;
    private final Map<String, String> UNIT_MAP;

    public CatalogueConverter(CatalogueDynamicConfig catalogueDynamicConfig) {
        this.NON_ATTRIBUTE_KEYS = new HashSet<>(catalogueDynamicConfig.getExcludedAttributes());
        this.UNIT_MAP = new HashMap<>(catalogueDynamicConfig.getUnitMap());
        ATTRIBUTE_KEYS = new HashSet<>(catalogueDynamicConfig.getIncludedAttributes());
    }

    // MAIN CONVERTER
    public SearchResponse convertGenericSearchToSearchResponse(
            ProductSearchResponse productSearchResponse, SearchRequest searchRequest) {

        try {
            List<Product> products =
                    Optional.ofNullable(productSearchResponse.getProducts())
                            .orElse(Collections.emptyList());

            SearchResponse response = new SearchResponse();

            // Dynamic Filters
            response.setFilterConditions(buildDynamicFilters(products));

            // searchAction flag logic
            if (searchRequest.isSearchAction()) {

                List<PLPCard> plpCards = products.stream()
                        .map(this::convertToPLPCard)
                        .collect(Collectors.toList());

                response.setProducts(plpCards);
                response.setCount((long) plpCards.size());
                response.setTotal(productSearchResponse.getTotalHits());
                response.setSuggestions(Collections.emptyList());

            } else {
                response.setProducts(Collections.emptyList());
                List<SearchSuggestion> suggestionList = products.stream()
                        .map(this::convertToSuggestion)
                        .toList();
                response.setCount((long) suggestionList.size());
                response.setTotal(productSearchResponse.getTotalHits());

                response.setFilterConditions(Collections.emptyList());
                response.setSuggestions(suggestionList);
            }

            response.setQuery(searchRequest.getText());
            response.setSearchAction(searchRequest.isSearchAction());
            response.setDescription(null);

            return response;

        } catch (Exception e) {
            log.error("Exception while mapping central catalogue search response → PLP: {}", e.getMessage(), e);
            throw new CentralCommerceServiceException(
                    "Exception occurred while mapping central catalogue response: " + e.getMessage()
            );
        }
    }

    private PLPCard convertToPLPCard(Product product) {

        Map<String, Object> attrs = product.getAttributes();

        String delivery = CatalogueUtil.str(attrs.getOrDefault("delivery time", ""));
        if (delivery.isBlank()) {
            delivery = CatalogueUtil.str(attrs.getOrDefault("delivery_time", ""));
        }

        return PLPCard.builder()
                .productTitle(CatalogueUtil.str(attrs.get("product_title")))
                .productSlug(CatalogueUtil.str(attrs.get("slug")))
                .brand(CatalogueUtil.str(attrs.get("brand")))
                .deliveryInfo(delivery)
                .distributedDeliveryInfo("Delivery in 30 - 45 days")
                .imageUrl(CatalogueUtil.extractImage(product))
                .altText(CatalogueUtil.extractAlt(product))
                .productAttributes(buildDynamicPLPAttributes(attrs))
                .productMaterialMasterId(product.getProductMmid())
                .priceRange(null)
                .build();
    }

    private SearchSuggestion convertToSuggestion(Product product) {
        Map<String, Object> attrs = product.getAttributes();

        return SearchSuggestion.builder()
                .suggestionText(CatalogueUtil.str(attrs.get("product_title")))
                .productSlug(CatalogueUtil.str(attrs.get("slug")))
                .imageUrl(CatalogueUtil.extractImage(product))
                .productMaterialMasterId(product.getProductMmid())
                .build();
    }

    // =======================
    // UNIT-ONLY PLP ATTRIBUTES
    // =======================
    private List<PLPAttribute> buildDynamicPLPAttributes(Map<String, Object> attrs) {

        List<PLPAttribute> finalList = new ArrayList<>();
        Map<String, Double> minMap = new HashMap<>();
        Map<String, Double> maxMap = new HashMap<>();

        attrs.forEach((key, val) -> {
            if (key.endsWith("_min")) {
                minMap.put(key.replace("_min", ""), CatalogueUtil.safeDouble(val));
            } else if (key.endsWith("_max")) {
                maxMap.put(key.replace("_max", ""), CatalogueUtil.safeDouble(val));
            }
        });

        // Add RANGE attributes (still allowed for PLP display)
        for (String base : minMap.keySet()) {
            String unit = CatalogueUtil.getUnitFor(base, UNIT_MAP);

            finalList.add(PLPAttribute.builder()
                    .displayName(CatalogueUtil.formatName(base))
                    .value(CatalogueUtil.formatRange(minMap.get(base), maxMap.get(base), unit))
                    .build());
        }

        // Add SINGLE numeric attributes only if unit exists
        attrs.forEach((key, val) -> {

            if (val == null) return;
            if (NON_ATTRIBUTE_KEYS.contains(key)) return;
            if (key.endsWith("_min") || key.endsWith("_max")) return;

            Double num = CatalogueUtil.safeDouble(val);
            if (num == null) return;

            String unit = CatalogueUtil.getUnitFor(key, UNIT_MAP);
            if (unit.isBlank()) return;

            finalList.add(PLPAttribute.builder()
                    .displayName(CatalogueUtil.formatName(key))
                    .value(CatalogueUtil.trim(num) + CatalogueUtil.unitSuffix(unit))
                    .build());
        });

        return finalList;
    }

    // ONLY KEYS IN ATTRIBUTE_KEYS WILL BE USED
    private List<ProductFilterConditions> buildDynamicFilters(List<Product> products) {

        Map<String, Set<String>> selectionValues = new HashMap<>();
        Map<String, Double> minCollector = new HashMap<>();
        Map<String, Double> maxCollector = new HashMap<>();

        for (Product p : products) {

            Map<String, Object> attrs = p.getAttributes();
            if (attrs == null) continue;

            for (var entry : attrs.entrySet()) {

                String key = entry.getKey();
                Object val = entry.getValue();

                if (!ATTRIBUTE_KEYS.contains(key)) continue;

                if (val == null) continue;

                // Collect range values (but not using them for CCS)
                if (key.endsWith("_min")) {
                    minCollector.put(key.replace("_min", ""), CatalogueUtil.safeDouble(val));
                    continue;
                }
                if (key.endsWith("_max")) {
                    maxCollector.put(key.replace("_max", ""), CatalogueUtil.safeDouble(val));
                    continue;
                }

                // Collect selection values
                selectionValues
                        .computeIfAbsent(key, x -> new TreeSet<>())
                        .add(String.valueOf(val));
            }
        }

        List<ProductFilterConditions> out = new ArrayList<>();

        //  RANGE FILTERS NOT SUPPORTED — COMMENTED
    /*
    for (String base : minCollector.keySet()) {

        TreeSet<String> values = new TreeSet<>();
        if (minCollector.get(base) != null) values.add(trim(minCollector.get(base)));
        if (maxCollector.get(base) != null) values.add(trim(maxCollector.get(base)));

        out.add(ProductFilterConditions.builder()
                .id(base.toUpperCase())
                .displayText(formatName(base))
                .type("range")
                .values(new ArrayList<>(values))
                .selectedValues(new ArrayList<>())
                .build());
    }
    */

        //  SELECTION FILTERS ONLY (SUPPORTED BY CCS)
        selectionValues.forEach((key, values) -> out.add(
                ProductFilterConditions.builder()
                        .id(key.toUpperCase())
                        .displayText(CatalogueUtil.formatName(key))
                        .type("selection")
                        .values(new ArrayList<>(values))
                        .selectedValues(new ArrayList<>())
                        .build()
        ));

        return out;
    }
}
