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
    public UomConvertResponse convertUom(List<PurchasedUom> requests) {

        if (requests == null || requests.isEmpty()) {
            log.error("UOM conversion request list cannot be null or empty");
            return new UomConvertResponse(Collections.emptyList());
        }

        // Extract all MMIDs
        List<String> variantMMIDList = requests.stream()
                .map(req -> Optional.ofNullable(req.getVariantMMID())
                        .filter(v -> !v.isEmpty())
                        .orElse(req.getProductMMID() + DEFAULT_VARIANT_SUFFIX))
                .toList();

        // Call master data for all MMIDs at once
        Map<String, Data> masterProductMap = masterDataClient.fetchProductDetails(variantMMIDList);

        if (masterProductMap == null) masterProductMap = Collections.emptyMap();

        List<UomConvert> uomConvertList = new ArrayList<>();

        for (PurchasedUom req : requests) {

            String variantOrDefault =
                    (req.getVariantMMID() == null || req.getVariantMMID().isEmpty())
                            ? req.getProductMMID() + DEFAULT_VARIANT_SUFFIX
                            : req.getVariantMMID();

            Data productData = masterProductMap.get(variantOrDefault);

            if (productData == null) {
                log.error("Product not found in master catalogue for mmid={}", req.getProductMMID());
                throw new CentralCommerceServiceException(
                        "Product not found in catalogue: " + req.getProductMMID(),
                        HttpStatus.NOT_FOUND);
            }
            try {
                uomConvertList.add(processUom(productData, req));
            } catch (CentralCommerceServiceException e) {
                log.error("UOM conversion failed for product={}, reason={}", req.getProductMMID(), e.getMessage(), e);
                throw e;
            } catch (Exception e) {
                log.error("Unexpected error while converting UOM for product={}", req.getProductMMID(), e);
                throw new CentralCommerceServiceException("Unexpected error converting UOM for " + req.getProductMMID(), HttpStatus.INTERNAL_SERVER_ERROR, e);
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


