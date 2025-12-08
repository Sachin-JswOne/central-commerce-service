package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.Attribute;
import com.jswone.commerce.core.model.PurchasedUom;
import com.jswone.commerce.core.model.UomConvert;
import com.jswone.commerce.core.model.UomValueDetails;
import com.jswone.commerce.core.model.masters.Category;
import com.jswone.commerce.core.model.masters.Data;
import com.jswone.commerce.core.model.masters.Uom;
import com.jswone.commerce.core.model.response.search.UomConvertResponse;
import com.jswone.commerce.core.service.MasterDataClient;
import com.jswone.commerce.core.service.UomConvertService;
import com.jswone.uom.convertor.bean.dto.operation.UnitConversionRequest;
import com.jswone.uom.convertor.bean.dto.operation.UnitConversionResponse;
import com.jswone.uom.convertor.service.UOMConvertorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;


@Slf4j
@Service
public class UomConvertServiceImpl implements UomConvertService {

    private static final String DEFAULT_VARIANT_SUFFIX = "-10000000";
    private final UOMConvertorService uomConvertorService;
    private final MasterDataClient masterDataClient;

    public UomConvertServiceImpl(UOMConvertorService uomConvertorService,
                                 MasterDataClient masterDataClient) {
        this.uomConvertorService = uomConvertorService;
        this.masterDataClient = masterDataClient;
    }

    @Override
    public UomConvertResponse convertUom(List<PurchasedUom> purchasedUomList) {

        List<UomConvert> uomConvertList = new ArrayList<>();

        if (purchasedUomList == null || purchasedUomList.isEmpty()) {
            log.error("UOM conversion request list cannot be null or empty");
            return UomConvertResponse.builder().uomConvertedProducts(Collections.emptyList()).build();
        }

        purchasedUomList.forEach(uom -> {

            Optional.ofNullable(uom.getPurchasedUom())
                    .map(Attribute::getName)
                    .ifPresent(name -> uom.getPurchasedUom().setName(name.toLowerCase()));

            Optional.ofNullable(uom.getProductAttributes())
                    .ifPresent(attrs -> attrs.forEach(attr -> {
                        if (attr.getName() != null) {
                            attr.setName(attr.getName().toLowerCase());
                        }
                    }));
        });

        List<String> variantMMIDList = purchasedUomList.stream()
                .map(req -> Optional.ofNullable(req.getVariantMMID())
                        .filter(v -> !v.isEmpty())
                        .orElse(req.getProductMMID() + DEFAULT_VARIANT_SUFFIX))
                .toList();

        Map<String, Data> masterProductMap = Optional.ofNullable(masterDataClient.fetchProductDetails(variantMMIDList))
                .orElse(Collections.emptyMap());

        for (PurchasedUom purchasedUom : purchasedUomList) {

            String variantOrDefault =
                    (purchasedUom.getVariantMMID() == null || purchasedUom.getVariantMMID().isEmpty())
                            ? purchasedUom.getProductMMID() + DEFAULT_VARIANT_SUFFIX
                            : purchasedUom.getVariantMMID();
            try {
                Data productData = masterProductMap.get(variantOrDefault);

                if (productData == null) {
                    throw new CentralCommerceServiceException(
                            "Product not found in Master Data with productMMID: " + purchasedUom.getProductMMID(),
                            HttpStatus.NOT_FOUND);
                }

                UomConvert uomConverted = processUom(productData, purchasedUom);
                uomConverted.setSuccess(true);
                uomConverted.setErrorMessage(null);

                uomConvertList.add(uomConverted);

            } catch (CentralCommerceServiceException e) {
                log.error("UOM conversion failed for productMMID={}", purchasedUom.getProductMMID(), e);
                uomConvertList.add(UomConvert.builder()
                        .productMMID(purchasedUom.getProductMMID())
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build());
            } catch (Exception e) {
                log.error("Unexpected error while converting UOM for productMMID={}", purchasedUom.getProductMMID(), e);
                uomConvertList.add(UomConvert.builder()
                        .productMMID(purchasedUom.getProductMMID())
                        .success(false)
                        .errorMessage("Unexpected Internal Error")
                        .build());
            }
        }

        return UomConvertResponse.builder().uomConvertedProducts(uomConvertList).build();
    }

    private UomConvert processUom(Data data, PurchasedUom req) {

        List<Uom> uomList = Optional.ofNullable(data.getCategory())
                .map(Category::getUom)
                .orElse(Collections.emptyList());

        String primaryUom = getUomByType(uomList, "primary");
        String secondaryUom = getUomByType(uomList, "secondary");

        Attribute purchasedUom = req.getPurchasedUom();

        if (purchasedUom == null || purchasedUom.getName() == null || purchasedUom.getValue() == null) {
            throw new CentralCommerceServiceException(
                    "Invalid purchasedUom for product " + req.getProductMMID(),
                    HttpStatus.BAD_REQUEST
            );
        }

        String sourceUom = req.getPurchasedUom().getName();
        double sourceQty = Double.parseDouble(req.getPurchasedUom().getValue());

        Map<String, Double> attributeMap = extractAttributes(req);

        UnitConversionResponse<Double> primaryResp =
                convertTo(primaryUom, sourceUom, sourceQty, attributeMap);

        UnitConversionResponse<Double> secondaryResp =
                convertTo(secondaryUom, sourceUom, sourceQty, attributeMap);

        return UomConvert.builder()
                .productMMID(req.getProductMMID())
                .primaryUom(buildUom(primaryUom, primaryResp, uomList))
                .secondaryUom(buildUom(secondaryUom, secondaryResp, uomList))
                .build();
    }

    private String getUomByType(List<Uom> list, String type) {
        return list.stream()
                .filter(u -> type.equalsIgnoreCase(u.getUomType()))
                .map(Uom::getName)
                .findFirst()
                .orElse(null);
    }

    private Map<String, Double> extractAttributes(PurchasedUom request) {

        Map<String, Double> map = new HashMap<>();

        if (request.getProductAttributes() == null) return map;

        for (Attribute attr : request.getProductAttributes()) {
            if (attr == null || attr.getName() == null || attr.getValue() == null)
                continue;
            try {
                map.put(attr.getName(), Double.valueOf(attr.getValue()));
            } catch (Exception e) {
                log.warn("Skipping invalid attribute {} due to parse error", attr);
            }
        }
        return map;
    }

    private UnitConversionResponse<Double> convertTo(
            String destinationUom,
            String sourceUom,
            double quantity,
            Map<String, Double> attributes) {

        if (destinationUom == null)
            return null;

        // No conversion required
        if (destinationUom.equalsIgnoreCase(sourceUom)) {
            return UnitConversionResponse.<Double>builder()
                    .uom(destinationUom)
                    .convertedQuantity(quantity)
                    .unRoundedConvertedQuantity(quantity)
                    .pricePerUnit(Optional.empty())
                    .convertedPrice(Optional.empty())
                    .build();
        }

        UnitConversionRequest req = UnitConversionRequest.builder()
                .sourceUom(sourceUom)
                .destinationUom(destinationUom)
                .quantity(quantity)
                .attributesMap(attributes)
                .pricePerDestinationUom(Optional.empty())
                .build();

        try {
            return uomConvertorService.convert(req);
        } catch (Exception e) {
            log.error("Error converting from {} → {} : {}", sourceUom, destinationUom, e.getMessage());
            throw new CentralCommerceServiceException(
                    "UOM conversion failed: " + sourceUom + " → " + destinationUom,
                    HttpStatus.INTERNAL_SERVER_ERROR, e
            );
        }
    }

    private Uom findUomDetails(List<Uom> uomList, String uomName) {
        if (uomList == null || uomName == null) return null;

        return uomList.stream()
                .filter(u -> uomName.equalsIgnoreCase(u.getName()))
                .findFirst()
                .orElse(null);
    }

    private UomValueDetails buildUom(
            String uomName,
            UnitConversionResponse<Double> resp,
            List<Uom> uomList) {

        if (uomName == null || resp == null)
            return null;

        Uom details = findUomDetails(uomList, uomName);

        return UomValueDetails.builder()
                .unit(uomName)
                .value(resp.getConvertedQuantity())
                .label(details != null ? details.getUiLabelQuantity() : null)
                .priceLabel(details != null ? details.getUiLabelPrice() : null)
                .build();
    }
}


