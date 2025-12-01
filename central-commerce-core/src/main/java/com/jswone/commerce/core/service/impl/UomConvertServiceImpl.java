package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.model.Attribute;
import com.jswone.commerce.core.model.masters.Category;
import com.jswone.commerce.core.model.masters.Data;
import com.jswone.commerce.core.model.masters.Uom;
import com.jswone.commerce.core.model.request.UomConvertRequest;
import com.jswone.commerce.core.model.response.UomConvertResponse;
import com.jswone.commerce.core.service.MasterDataClient;
import com.jswone.commerce.core.service.UomConvertService;
import com.jswone.uom.convertor.bean.dto.operation.UnitConversionRequest;
import com.jswone.uom.convertor.bean.dto.operation.UnitConversionResponse;
import com.jswone.uom.convertor.service.UOMConvertorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;


@Slf4j
@Service
public class UomConvertServiceImpl implements UomConvertService {

    private final UOMConvertorService uomConvertorService;
    private final MasterDataClient masterDataClient;

    public UomConvertServiceImpl(UOMConvertorService uomConvertorService,
                                 MasterDataClient masterDataClient) {
        this.uomConvertorService = uomConvertorService;
        this.masterDataClient = masterDataClient;
    }

    @Override
    public List<UomConvertResponse> convertUom(List<UomConvertRequest> requests) {

        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("UOM conversion request list cannot be null or empty");
        }

        // Extract all MMIDs
        List<String> productMMIDs = requests.stream()
                .map(UomConvertRequest::getProductMMID)
                .toList();

        // Call master data for all MMIDs at once
        Map<String, Data> masterProductMap = masterDataClient.fetchProductDetails(productMMIDs);

        List<UomConvertResponse> uomConvertResponseList = new ArrayList<>();

        for (UomConvertRequest req : requests) {

            Data productData = masterProductMap.get(req.getProductMMID());

            if (productData == null) {
                log.error("Product not found in catalogue for mmid={}", req.getProductMMID());
                throw new RuntimeException("Product not found in catalogue: " + req.getProductMMID());
            }

            uomConvertResponseList.add(processUom(productData, req));
        }

        return uomConvertResponseList;
    }

    private UomConvertResponse processUom(Data data, UomConvertRequest req) {

        List<Uom> uomList = Optional.ofNullable(data.getCategory())
                .map(Category::getUom)
                .orElse(Collections.emptyList());

        String primaryUom = getUomByType(uomList, "primary");
        String secondaryUom = getUomByType(uomList, "secondary");

        String sourceUom = req.getCustomAttribute().getName();
        double sourceQty = Double.parseDouble(req.getCustomAttribute().getValue());

        Map<String, Double> attributeMap = extractAttributes(req);

        UnitConversionResponse<Double> primaryResp =
                convertTo(primaryUom, sourceUom, sourceQty, attributeMap);

        UnitConversionResponse<Double> secondaryResp =
                convertTo(secondaryUom, sourceUom, sourceQty, attributeMap);

        return UomConvertResponse.builder()
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

    private Map<String, Double> extractAttributes(UomConvertRequest request) {

        Map<String, Double> map = new HashMap<>();

        if (request.getProductAttributes() == null) return map;

        for (Attribute attr : request.getProductAttributes()) {
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
            throw new RuntimeException("UOM conversion failed", e);
        }
    }

    private Uom findUomDetails(List<Uom> uomList, String uomName) {
        return uomList.stream()
                .filter(u -> uomName.equalsIgnoreCase(u.getName()))
                .findFirst()
                .orElse(null);
    }

    private com.jswone.commerce.core.model.Uom buildUom(
            String uomName,
            UnitConversionResponse<Double> resp,
            List<Uom> uomList) {

        if (uomName == null || resp == null)
            return null;

        Uom details = findUomDetails(uomList, uomName);

        return com.jswone.commerce.core.model.Uom.builder()
                .unit(uomName)
                .value(resp.getConvertedQuantity())
                .label(details != null ? details.getUiLabelQuantity() : null)
                .priceLabel(details != null ? details.getUiLabelPrice() : null)
                .build();
    }
}


