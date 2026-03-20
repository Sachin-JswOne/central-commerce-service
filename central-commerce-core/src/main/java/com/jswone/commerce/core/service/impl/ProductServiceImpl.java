package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.entity.catalogue.Attribute;
import com.jswone.commerce.core.entity.catalogue.ProductCatalogueStore;
import com.jswone.commerce.core.entity.catalogue.Variant;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.exceptions.ProductSelectorException;
import com.jswone.commerce.core.mapper.BreadcrumbMapper;
import com.jswone.commerce.core.mapper.ProductSlugMapper;
import com.jswone.commerce.core.model.centralCatalogue.ProductSlug;
import com.jswone.commerce.core.model.centralCatalogue.ProductTypeData;
import com.jswone.commerce.core.model.centralCatalogue.QuantityCard;
import com.jswone.commerce.core.model.centralCatalogue.VariantSelector;
import com.jswone.commerce.core.model.request.ProductAttributeDTO;
import com.jswone.commerce.core.model.request.ProductSkuRequest;
import com.jswone.commerce.core.model.request.ProductTypeBulkRequest;
import com.jswone.commerce.core.model.response.ProductSelectorSkuResponse;
import com.jswone.commerce.core.model.response.SkuInfo;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductBulkResponse;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductTypeBulkResponse;
import com.jswone.commerce.core.repository.ProductCatalogueStoreRepository;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.service.ProductService;
import com.jswone.commerce.core.util.ProductAttributeUtil;
import com.jswone.commons.constants.JSWGenericConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.jswone.commerce.core.constants.GenericConstants.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductCatalogueStoreRepository productCatalogueStoreRepository;
    private final ProductAttributeUtil productAttributeUtil;
    private final CentralCatalogueClient centralCatalogueClient;
    private final ProductSlugMapper productSlugMapper;


    @Override
    public SkuInfo getMatchedVariantResponse(ProductSkuRequest productSkuRequest) {
        List<ProductAttributeDTO> productAttributeDTOS = productSkuRequest.getProductAttributes();
        List<ProductAttributeDTO> newProductAttributeDTO = productAttributeUtil.convertAttributes(productAttributeDTOS);
        productSkuRequest = productSkuRequest.toBuilder().productAttributes(newProductAttributeDTO).build();
        return getMatchedVariant(productSkuRequest);
    }

    @Override
    public ProductSlug getProductFromSlug(String slug, String storeFront) {
        try{

            ProductBulkResponse productBulkResponse = centralCatalogueClient.getProductFromSlug(slug,"msme");

            if(Objects.isNull(productBulkResponse) || productBulkResponse.getProducts().isEmpty()){
                throw new CentralCommerceServiceException("Product is not available for slug : "+slug, HttpStatus.BAD_GATEWAY);
            }

            if(Objects.isNull(productBulkResponse.getProducts().getFirst().getProductTypeId())){
                throw new CentralCommerceServiceException("Product type Id is not available for slug : "+slug, HttpStatus.BAD_GATEWAY);
            }
            String productTypeId = productBulkResponse.getProducts().getFirst().getProductTypeId();

            ProductTypeBulkRequest request = new ProductTypeBulkRequest(Set.of(productTypeId), storeFront);

            ProductTypeBulkResponse productTypeBulkResponse = centralCatalogueClient.bulkTypeIdResponse(request);

            if(Objects.isNull(productTypeBulkResponse) || productTypeBulkResponse.getData().getProductTypeDetail().isEmpty()){
                throw new CentralCommerceServiceException("Product type is not available for slug : "+slug, HttpStatus.BAD_GATEWAY);
            }

            if(Objects.isNull(productTypeBulkResponse.getData().getProductTypeDetail().get(productTypeId).getQuantityCards())){
                throw new CentralCommerceServiceException("Quantity cards are not available for slug : "+slug, HttpStatus.BAD_GATEWAY);
            }

            List<QuantityCard> quantityCards = productTypeBulkResponse.getData().getProductTypeDetail().get(productTypeId).getQuantityCards();

            ProductSlug productSlug = productSlugMapper.toProductSlug(productBulkResponse.getProducts().getFirst(),quantityCards);

            handleAttributeMappingsAndUpdateProductSlug(productTypeBulkResponse, productTypeId, productSlug);
            productSlug.setProductOverview(productTypeBulkResponse.getData().getProductOverview());
            productSlug.setPdpIdentifier(pdpIdentifier(productSlug.getVariantSelectors()));

            return productSlug;

        } catch (Exception e) {
            throw new CentralCommerceServiceException(e.getLocalizedMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    private boolean pdpIdentifier(Map<String, VariantSelector> variantSelectors){
        List<String> uniqueInputTypes =
                Optional.ofNullable(variantSelectors)
                        .map(Map::values)
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(VariantSelector::getInputType)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        return uniqueInputTypes.stream()
                .anyMatch(s -> s.equalsIgnoreCase(CENTRAL_CATALOGUE_PDP_IDENTIFIER));
    }

    private void handleAttributeMappingsAndUpdateProductSlug(ProductTypeBulkResponse productTypeBulkResponse, String productTypeId, ProductSlug productSlug) {
        var variantSelectors = productTypeBulkResponse.getData().getProductTypeDetail().get(productTypeId).getVariantSelectors();

        var standardAttributes = productTypeBulkResponse.getData().getProductTypeDetail().get(productTypeId).getStandardAttributes();

        var customAttributes = productTypeBulkResponse.getData().getProductTypeDetail().get(productTypeId).getAttributes();

        Map<String,String> standardAttributeNames = standardAttributes.stream().filter(Objects::nonNull)
                .filter(sa -> sa.get(CENTRAL_CATALOGUE_STANDARD_ATTRIBUTE_NAME_KEY) != null)
                .collect(Collectors.toMap(
                        sa -> String.valueOf(sa.get(CENTRAL_CATALOGUE_STANDARD_ATTRIBUTE_NAME_KEY)),
                        sa -> String.valueOf(sa.get(CENTRAL_CATALOGUE_STANDARD_ATTRIBUTE_LABEL_KEY))
                ));

        Map<String,String> standardAttributeUnit = standardAttributes.stream().filter(Objects::nonNull)
                .filter(sa -> sa.get(CENTRAL_CATALOGUE_STANDARD_ATTRIBUTE_NAME_KEY) != null)
                .collect(Collectors.toMap(
                        sa -> String.valueOf(sa.get(CENTRAL_CATALOGUE_STANDARD_ATTRIBUTE_NAME_KEY)),
                        sa -> String.valueOf(sa.get(CENTRAL_CATALOGUE_STANDARD_ATTRIBUTE_UNIT_KEY))
                ));

        productSlugMapper.updateVariantSelectors(variantSelectors, productSlug.getAttributes(), standardAttributeNames, standardAttributeUnit);
        productSlugMapper.updateCustomAttributes(customAttributes, productSlug.getAttributes());

        productSlug.setVariantSelectors(variantSelectors);
        productSlug.setStandardAttributes(standardAttributes);
        productSlug.setCustomAttributes(customAttributes);
    }

    private SkuInfo getMatchedVariant(ProductSkuRequest productSkuRequest) {
        try {
            log.info(
                    "Making request to get matched variant from product catalogue store for product:{}",
                    productSkuRequest.getProductMaterialMasterId());
            ProductCatalogueStore productCatalogueStore = null;
            productCatalogueStore = productCatalogueStoreRepository.findProductCatalogueStoresByProductMaterialMasterId(
                    productSkuRequest.getProductMaterialMasterId());
            if(Objects.isNull(productCatalogueStore)){
                throw new CentralCommerceServiceException("Product not available in catalogue store : "
                        .concat(productSkuRequest.getProductMaterialMasterId()),HttpStatus.BAD_REQUEST);
            }
            ProductSelectorSkuResponse productSkuRes =
                    Objects.nonNull(productSkuRequest.getProductAttributes())
                            ? getVariant(
                            productCatalogueStore, productSkuRequest.getProductAttributes())
                            : null;
            return Optional.ofNullable(productSkuRes)
                    .map(
                            productSkuResponse -> {
                                if (productSkuResponse.getMatchedSkus().size() == 1)
                                    return productSkuResponse.getMatchedSkus().getFirst();
                                else if (productSkuResponse.getMatchedSkus().isEmpty())
                                    return productSkuResponse.getMasterSku();
                                else
                                    throw new ProductSelectorException(
                                            productSkuRequest,
                                            "No Unique SKU found for provided input ",
                                            HttpStatus.BAD_REQUEST);
                            })
                    .orElseThrow(
                            () ->
                                    new ProductSelectorException(
                                            productSkuRequest,
                                            "Error while fetching variant using product-selector",
                                            HttpStatus.SERVICE_UNAVAILABLE));
        } catch (CentralCommerceServiceException e) {
            throw new CentralCommerceServiceException(e.getLocalizedMessage(),HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            throw new ProductSelectorException(productSkuRequest, e.getLocalizedMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    public ProductSelectorSkuResponse getVariant(
            ProductCatalogueStore productCatalogueStore, List<ProductAttributeDTO> variantAttributes) {

        ProductSelectorSkuResponse productSkuResponse = new ProductSelectorSkuResponse();

        List<Attribute> attributes =
                variantAttributes.stream()
                        .map(productAttributeUtil::transformToAttribute)
                        .collect(Collectors.toList());

        List<String> availableSkus = new ArrayList<>();
        List<SkuInfo> matchedSkus = new ArrayList<>();

        String variantMasterId =
                Optional.ofNullable(productCatalogueStore.getMasterVariant())
                        .map(Variant::getMmId)
                        .orElse(null);

        if (StringUtils.isEmpty(variantMasterId)) {
            String productMmId = productCatalogueStore.getProductMaterialMasterId();
            variantMasterId =
                    productMmId != null
                            ? productMmId.concat("-").concat("10000000")
                            : JSWGenericConstants.EMPTY_STRING;
        }

        if (productCatalogueStore.getVariants() != null
                && !productCatalogueStore.getVariants().isEmpty()) {
            for (Variant variant : productCatalogueStore.getVariants()) {
                String variantMMId =
                        StringUtils.defaultIfEmpty(
                                variant.getMmId(), JSWGenericConstants.EMPTY_STRING);

                if (variantMMId.equals(JSWGenericConstants.EMPTY_STRING)) {
                    variantMMId =
                            productCatalogueStore
                                    .getProductMaterialMasterId()
                                    .concat("-")
                                    .concat("10000000");
                }

                availableSkus.add(variant.getVariantKey());

                Pair<Boolean, List<Attribute>> variantCustomAttributePair =
                        isMatchProductVariant(variant, attributes);

                if (variantCustomAttributePair.getLeft()) {
                    matchedSkus.add(
                            SkuInfo.builder()
                                    .variantKey(variant.getVariantKey())
                                    .customAttribute(variantCustomAttributePair.getRight())
                                    .variantMMID(variantMMId)
                                    .productTypeKey(productCatalogueStore.getProductTypeKey())
                                    .sku(variant.getSku())
                                    .productKey(productCatalogueStore.getProductKey())
                                    .build());
                }
            }
        } else {
            log.error(
                    "No variants found for product key: {}", productCatalogueStore.getProductKey());
        }

        Variant masterVariant = productCatalogueStore.getMasterVariant();
        if (masterVariant != null) {
            Pair<Boolean, List<Attribute>> masterVariantAttributePair =
                    isMatchProductVariant(masterVariant, attributes);

            if (matchedSkus.isEmpty() && masterVariantAttributePair.getLeft()) {
                matchedSkus.add(
                        SkuInfo.builder()
                                .variantKey(masterVariant.getVariantKey())
                                .customAttribute(masterVariantAttributePair.getRight())
                                .variantMMID(variantMasterId)
                                .productTypeKey(productCatalogueStore.getProductTypeKey())
                                .sku(masterVariant.getSku())
                                .productKey(productCatalogueStore.getProductKey())
                                .build());
            }
        }

        productSkuResponse.setId(
                productCatalogueStore.getIdentifier() != null
                        ? productCatalogueStore.getIdentifier().toString()
                        : null);
        productSkuResponse.setProductKey(productCatalogueStore.getProductKey());
        productSkuResponse.setProductName(productCatalogueStore.getProductTitle());
        productSkuResponse.setMatchedSkus(matchedSkus);
        productSkuResponse.setAvailableSkuKeys(availableSkus);

        if (masterVariant != null) {
            productSkuResponse.setMasterSku(
                    SkuInfo.builder()
                            .variantKey(masterVariant.getVariantKey())
                            .customAttribute(attributes)
                            .variantMMID(variantMasterId)
                            .productTypeKey(productCatalogueStore.getProductTypeKey())
                            .sku(masterVariant.getSku())
                            .productKey(productCatalogueStore.getProductKey())
                            .build());
        }

        return productSkuResponse;
    }

    public static Pair<Boolean, List<Attribute>> isMatchProductVariant(
            Variant variant, List<Attribute> attributes) {

        if (variant == null || variant.getAttributes() == null) {
            return Pair.of(false, Collections.emptyList());
        }

        boolean productFlag =
                attributes.stream()
                        .allMatch(
                                a ->
                                        variant.getAttributes().stream()
                                                .anyMatch(va -> checkEquality(va, a)));

        return productFlag
                ? Pair.of(true, Collections.emptyList())
                : checkForRangeBasedMatch(attributes, variant);
    }

    protected static Pair<Boolean, List<Attribute>> checkForRangeBasedMatch(
            List<Attribute> attributes, Variant variant) {

        boolean productFlag = false;
        List<Attribute> customAttributes = new ArrayList<>();
        List<Boolean> flatList = new ArrayList<>();

        attributes.forEach(
                val -> {
                    ProductAttributeDTO minAttribute = new ProductAttributeDTO();
                    ProductAttributeDTO maxAttribute = new ProductAttributeDTO();

                    String attr = val.getName();
                    int lastIndexOf = attr.lastIndexOf("_");

                    if (lastIndexOf == -1) {
                        flatList.add(
                                variant.getAttributes().stream()
                                        .anyMatch(va -> checkEquality(va, val)));
                        return;
                    }

                    String baseAttr = attr.substring(0, lastIndexOf);
                    String unitPart = attr.substring(attr.lastIndexOf("_") + 1);

                    String minAttr = baseAttr + "_MIN_" + unitPart;
                    String maxAttr = baseAttr + "_MAX_" + unitPart;
                    String otherMinAttr = attr + "_MIN";
                    String otherMaxAttr = attr + "_MAX";

                    variant.getAttributes()
                            .forEach(
                                    va -> {
                                        if (va.getName().equals(minAttr)
                                                || va.getName().equals(otherMinAttr)) {
                                            minAttribute.setKey(va.getName());
                                            minAttribute.setValue(va.getValue());
                                        } else if (va.getName().equals(maxAttr)
                                                || va.getName().equals(otherMaxAttr)) {
                                            maxAttribute.setKey(va.getName());
                                            maxAttribute.setValue(va.getValue());
                                        }
                                    });

                    if (minAttribute.getValue() != null
                            && maxAttribute.getValue() != null
                            && Comparable.class.isAssignableFrom(val.getValue().getClass())
                            && Comparable.class.isAssignableFrom(minAttribute.getValue().getClass())
                            && Comparable.class.isAssignableFrom(
                            maxAttribute.getValue().getClass())) {

                        double value = Double.parseDouble(val.getValue().toString());
                        double minValue = Double.parseDouble(minAttribute.getValue().toString());
                        double maxValue = Double.parseDouble(maxAttribute.getValue().toString());

                        if (value >= minValue && value <= maxValue) {
                            flatList.add(true);
                            customAttributes.add(val);
                        } else {
                            flatList.add(false);
                        }
                    } else {
                        flatList.add(
                                variant.getAttributes().stream()
                                        .anyMatch(va -> checkEquality(va, val)));
                    }
                });

        if (!flatList.contains(false)) {
            productFlag = true;
        }

        return Pair.of(productFlag, customAttributes);
    }

    private static boolean checkEquality(
            Attribute va, Attribute a) {
        if (va == null || a == null || va.getValue() == null || a.getValue() == null) {
            return false;
        }

        if (!va.getName().equalsIgnoreCase(a.getName())) {
            return false;
        }

        Object vaValue = va.getValue();
        Object aValue = a.getValue();

        if (isNumeric(vaValue) && isNumeric(aValue)) {
            double vaNum = Double.parseDouble(String.valueOf(vaValue));
            double aNum = Double.parseDouble(String.valueOf(aValue));
            return Double.compare(vaNum, aNum) == 0;
        }

        if (vaValue instanceof String && aValue instanceof String) {
            return ((String) vaValue).trim().equalsIgnoreCase(((String) aValue).trim());
        }

        return vaValue.equals(aValue);
    }

    private static boolean isNumeric(Object value) {
        if (value == null) return false;
        if (value instanceof Number) return true;

        try {
            Double.parseDouble(value.toString());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
