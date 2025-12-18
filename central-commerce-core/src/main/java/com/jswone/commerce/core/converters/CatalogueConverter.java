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
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.util.CatalogueUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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
        this.ATTRIBUTE_KEYS = new HashSet<>(catalogueDynamicConfig.getIncludedAttributes());
    }

    // MAIN CONVERTER ======================================================================================
    public SearchResponse convertGenericSearchToSearchResponse(
            ProductSearchResponse productSearchResponse, ProductSearchResponse facetsResponse, SearchRequest searchRequest) {

        try {
            List<Product> products = Optional.ofNullable(productSearchResponse.getProducts())
                    .orElse(Collections.emptyList());

            SearchResponse response = new SearchResponse();

            // Dynamic Filters
            response.setFilterConditions(buildDynamicFilters(facetsResponse, searchRequest));

            // searchAction logic
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

                List<SearchSuggestion> suggestions = products.stream()
                        .map(this::convertToSuggestion)
                        .toList();

                response.setCount((long) suggestions.size());
                response.setTotal(productSearchResponse.getTotalHits());
                response.setFilterConditions(Collections.emptyList());
                response.setSuggestions(suggestions);
            }

            response.setQuery(searchRequest.getText());
            response.setSearchAction(searchRequest.isSearchAction());
            response.setDescription(null);

            return response;

        } catch (Exception e) {
            log.error("Exception while mapping search response: {}", e.getMessage(), e);
            throw new CentralCommerceServiceException(
                    "Exception occurred while mapping central catalogue response: " + e.getMessage()
            );
        }
    }

    // PLP CARD ============================================================================================
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

    // PLP ATTRIBUTE BUILDING ==============================================================================
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

        // Range values
        for (String base : minMap.keySet()) {
            String unit = CatalogueUtil.getUnitFor(base, UNIT_MAP);
            finalList.add(PLPAttribute.builder()
                    .displayName(CatalogueUtil.formatName(base))
                    .value(CatalogueUtil.formatRange(minMap.get(base), maxMap.get(base), unit))
                    .build());
        }

        // Single numeric values
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

    // FILTER BUILDER ======================================================================================
    private List<ProductFilterConditions> buildDynamicFilters(ProductSearchResponse facetResponse, SearchRequest searchRequest) {

        List<ProductFilterConditions> out = new ArrayList<>();

        // Build and return full facets
        Map<String, Set<String>> facetValues = new HashMap<>();

        //  Extract facet data from the response
        for (Map.Entry<String, Set<String>> facet : facetResponse.getFacets().entrySet()) {
            String key = facet.getKey();
            Set<String> val = facet.getValue();

            if (!ATTRIBUTE_KEYS.contains(key)) continue;
            if (val.isEmpty()) continue;

            facetValues.put(key, val);
        }

        // Build the response list by facets only
        facetValues.forEach((key, values) -> {
            out.add(
                    ProductFilterConditions.builder()
                            .id(key.toUpperCase())
                            .displayText(CatalogueUtil.formatName(key))
                            .type("selection")
                            .values(new ArrayList<>(values)) // Available values from facets
                            .selectedValues(new ArrayList<>()) // No selected values
                            .build()
            );
        });


        // Construct the output list only from the filters passed in the request.
        if (searchRequest.getFilterConditions() != null && !searchRequest.getFilterConditions().isEmpty()) {
            // READ SELECTED VALUES FROM FE
            Map<String, List<String>> selectedFromRequestMap = getSelectedFromRequestMap(searchRequest);

            // BUILD FINAL FILTERS
            facetValues.forEach((key, values) -> {
                List<String> selected = selectedFromRequestMap.getOrDefault(
                        key.toLowerCase(),
                        Collections.emptyList()
                );
                out.forEach(productFilterConditions -> {
                    if (productFilterConditions.getId().equalsIgnoreCase(key)) {
                        productFilterConditions.setSelectedValues(selected);
                    }
                });
            });
        }
        return out;
    }

    private static Map<String, List<String>> getSelectedFromRequestMap(SearchRequest searchRequest) {
        Map<String, List<String>> selectedFromRequestMap = new HashMap<>();

        if (searchRequest.getFilterConditions() != null && !searchRequest.getFilterConditions().isEmpty()) {
            for (ProductFilterConditions reqFilter : searchRequest.getFilterConditions()) {

                if (!"selection".equalsIgnoreCase(reqFilter.getType())) continue;

                List<String> selected = reqFilter.getSelectedValues() == null
                        ? Collections.emptyList()
                        : reqFilter.getSelectedValues();

                selectedFromRequestMap.put(reqFilter.getId().toLowerCase(), selected);
            }
        }
        return selectedFromRequestMap;
    }

}
