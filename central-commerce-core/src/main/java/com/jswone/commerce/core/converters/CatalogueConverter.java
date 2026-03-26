package com.jswone.commerce.core.converters;

import com.jswone.commerce.core.config.CatalogueDynamicConfig;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.CategoryTreeResponse;
import com.jswone.commerce.core.model.centralCatalogue.Product;
import com.jswone.commerce.core.model.request.FilterRequestProvider;
import com.jswone.commerce.core.model.request.ProductListingRequest;
import com.jswone.commerce.core.model.request.Search.SearchRequest;
import com.jswone.commerce.core.model.response.ProductListingResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.FacetsProvider;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductListingCatalogueResponse;
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
import java.util.stream.Stream;

import static com.jswone.commerce.core.constants.GenericConstants.SELECTION;

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
            ProductSearchResponse productSearchResponse,
            SearchRequest searchRequest,
            CategoryTreeResponse categoryTreeResponse,
            ProductFilterConditions categoryFilterConditions) {

        try {
            List<Product> products = Optional.ofNullable(productSearchResponse.getProducts())
                    .orElse(Collections.emptyList());

            SearchResponse response = new SearchResponse();

            // Dynamic Filters
            response.setFilterConditions(buildDynamicFiltersProductListing(productSearchResponse, searchRequest));
            if(Objects.nonNull(categoryTreeResponse) &&
                    Objects.nonNull(categoryTreeResponse.getNavigation()) &&
                    !categoryTreeResponse.getNavigation().isEmpty()) {
                response.getFilterConditions().add(ProductFilterConditions.builder()
                        .displayText("Category")
                        .id("CATEGORY")
                        .selectedValues(new ArrayList<>())
                        .type(SELECTION)
                        .values(categoryTreeResponse.getNavigation())
                        .build());
            }else if(Objects.nonNull(categoryFilterConditions)){

                response.getFilterConditions().add(categoryFilterConditions);

            }

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
                .categorySlugs(getCategorySlugs(product))
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

    private static Map<String, List<String>> getSelectedFromRequestMap(FilterRequestProvider filterRequest) {
        Map<String, List<String>> selectedFromRequestMap = new HashMap<>();

        if (filterRequest.getFilterConditions() != null && !filterRequest.getFilterConditions().isEmpty()) {
            for (ProductFilterConditions reqFilter : filterRequest.getFilterConditions()) {

                if (!SELECTION.equalsIgnoreCase(reqFilter.getType())) continue;

                List<String> selected = reqFilter.getSelectedValues() == null
                        ? Collections.emptyList()
                        : reqFilter.getSelectedValues();

                selectedFromRequestMap.put(reqFilter.getId().toLowerCase(), selected);
            }
        }
        return selectedFromRequestMap;
    }

    // PRODUCT LISTING CONVERTER ======================================================================================
    public ProductListingResponse convertCataloguePLPResponseToPLPResponse(
            ProductListingCatalogueResponse listingCatalogueResponse,
            ProductListingRequest listingRequest,
            CategoryTreeResponse categoryTreeResponse,
            ProductFilterConditions categoryFilterConditions) {

        try {
            List<Product> products = Optional.ofNullable(listingCatalogueResponse.getProducts())
                    .orElse(Collections.emptyList());

            ProductListingResponse response = new ProductListingResponse();

            // Dynamic Filters
            response.setFilterConditions(buildDynamicFiltersProductListing(listingCatalogueResponse, listingRequest));
            if(Objects.nonNull(categoryTreeResponse) &&
               Objects.nonNull(categoryTreeResponse.getNavigation()) &&
               !categoryTreeResponse.getNavigation().isEmpty()) {
                response.getFilterConditions().add(ProductFilterConditions.builder()
                        .displayText("Category")
                        .id("CATEGORY")
                        .selectedValues(new ArrayList<>())
                        .type(SELECTION)
                        .values(categoryTreeResponse.getNavigation())
                        .build());
            }else if(Objects.nonNull(categoryFilterConditions)){
                response.getFilterConditions().add(categoryFilterConditions);
            }
            List<PLPCard> plpCards = products.stream()
                    .map(this::convertToPLPCard)
                    .toList();

            response.setProducts(plpCards);
            response.setCount((long) plpCards.size());
            response.setTotal(listingCatalogueResponse.getTotalHits());
            response.setCategoryId(listingRequest.getCategoryId());

            return response;

        } catch (Exception e) {
            log.error("Exception while mapping product listing response: {}", e.getMessage(), e);
            throw new CentralCommerceServiceException(
                    "Exception occurred while mapping central catalogue product listing response: " + e.getMessage()
            );
        }
    }

    private List<String> getCategorySlugs(Product product){
        return product.getAssociatedCategories()
                .stream()
                .flatMap(category -> {

                    Stream<String> categorySlug = Stream.ofNullable(
                            (String) category.getAttributes().get("slug")
                    );

                    Stream<String> traversalSlugs = category.getTraversal() == null
                            ? Stream.empty()
                            : category.getTraversal().stream()
                            .map(traversal ->
                                    (String) traversal.getAttributes().get("slug")
                            );

                    return Stream.concat(categorySlug, traversalSlugs);
                })
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    // FILTER BUILDER PRODUCT LISTING======================================================================================
    private List<ProductFilterConditions> buildDynamicFiltersProductListing(FacetsProvider facetResponse, FilterRequestProvider filterRequest) {

        List<ProductFilterConditions> out = new ArrayList<>();

        // Build and return full facets
        Map<String, Set<String>> facetValues = new HashMap<>();

        //  Extract facet data from the response
        for (Map.Entry<String, Set<String>> facet : facetResponse.getFacets().entrySet()) {
            String key = facet.getKey();
            Set<String> val = facet.getValue();

            if (!ATTRIBUTE_KEYS.contains(key)) continue;
            if (val == null || val.isEmpty()) continue;

            facetValues.put(key, val);
        }

        // Build the response list by facets only
        facetValues.forEach((key, values) -> {
            ProductFilterConditions filterConditions =
                    Optional.ofNullable(filterRequest.getFilterConditions())
                            .orElse(Collections.emptyList())
                            .stream()
                            .filter(fc -> fc.getId().equalsIgnoreCase(key.toUpperCase()))
                            .filter(fc -> fc.getSelectedValues() != null && !fc.getSelectedValues().isEmpty())
                            .findAny()
                            .orElse(null);
            if(Objects.nonNull(filterConditions) && Objects.nonNull(filterConditions.getId())){
                out.add(filterConditions);
            }else {
                out.add(
                        ProductFilterConditions.builder()
                                .id(key.toUpperCase())
                                .displayText(CatalogueUtil.formatName(key))
                                .type(SELECTION)
                                .values(new ArrayList<>(values)) // Available values from facets
                                .selectedValues(new ArrayList<>()) // No selected values
                                .build()
                );
            }
        });


        // Construct the output list only from the filters passed in the request.
        if (filterRequest.getFilterConditions() != null && !filterRequest.getFilterConditions().isEmpty()) {
            // READ SELECTED VALUES FROM FE
            Map<String, List<String>> selectedFromRequestMap = getSelectedFromRequestMap(filterRequest);

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
}
