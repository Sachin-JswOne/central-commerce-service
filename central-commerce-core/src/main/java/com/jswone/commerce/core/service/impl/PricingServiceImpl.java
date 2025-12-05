package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.Attribute;
import com.jswone.commerce.core.model.PurchasedUom;
import com.jswone.commerce.core.model.UomConvert;
import com.jswone.commerce.core.model.UomValueDetails;
import com.jswone.commerce.core.model.pricing.Item;
import com.jswone.commerce.core.model.pricing.LineItemPrice;
import com.jswone.commerce.core.model.pricing.LineItemPriceDto;
import com.jswone.commerce.core.model.pricing.PriceRequest;
import com.jswone.commerce.core.model.response.search.UomConvertResponse;
import com.jswone.commerce.core.rest.PricingServiceClient;
import com.jswone.commerce.core.service.PricingService;
import com.jswone.commerce.core.service.UomConvertService;
import com.jswone.commerce.core.util.PricingUtil;
import com.jswone.commons.pricing.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static com.jswone.commerce.core.constants.GenericConstants.*;

@Service
@Log4j2
public class PricingServiceImpl implements PricingService {
    private final UomConvertService uomConvertService;
    private final PricingServiceClient pricingServiceClient;

    public PricingServiceImpl(UomConvertService uomConvertService, PricingServiceClient pricingServiceClient) {
        this.uomConvertService = uomConvertService;
        this.pricingServiceClient = pricingServiceClient;
    }

    @Override
    public List<LineItemPrice> getPrice(PriceRequest priceRequest){
        try{
            Set<String> seen = new HashSet<>();

            priceRequest.getItems().removeIf(item ->
                    !seen.add(item.getProductMMID())
            );
            List<LineItemPrice> lineItemPrices = new ArrayList<>();
            List<PurchasedUom> purchasedUoms = priceRequest.getItems().stream()
                    .map(price -> PurchasedUom.builder()
                            .productMMID(price.getProductMMID())
                            .purchasedUom(Attribute.builder()
                                    .name(price.getPurchasedQuantity().getKey())
                                    .value(price.getPurchasedQuantity().getValue())
                                    .build())
                            .productAttributes(new HashSet<>(price.getProductAttributes()))
                            .build())
                    .toList();

            UomConvertResponse uomConvertResponse = uomConvertService.convertUom(purchasedUoms);

            if(Objects.isNull(uomConvertResponse) || uomConvertResponse.getUomConvertedProducts().isEmpty()){
                throw new CentralCommerceServiceException("UOM conversion failed", HttpStatus.BAD_REQUEST);
            }

            Map<String, UomConvert> productUomMap = getProductUomMap(priceRequest, uomConvertResponse);

            if(productUomMap.size() != priceRequest.getItems().size()){
                throw new CentralCommerceServiceException("UOM conversion provided incorrect data", HttpStatus.BAD_REQUEST);
            }

            ProductPricingRequest productPricingRequest = this.getPricingProductRequest(priceRequest,productUomMap);

            PricingServiceResponse priceServiceResponse =
                    pricingServiceClient.callPricingService(productPricingRequest);

            if(Boolean.FALSE.equals(priceServiceResponse.getSuccess())){
                return List.of(LineItemPrice.builder()
                        .success(false)
                        .displayErrorMessage(PRICE_FETCH_DISPLAY_ERROR_MESSAGE)
                        .errorMessage(priceServiceResponse.getError().getMessage())
                        .minMoq(0)
                        .build());
            }
            Map<String, Vendor> priceVendor = getPriceVendorMap(priceServiceResponse);

            priceRequest.getItems().forEach(item -> {
                Vendor vendor = priceVendor.get(item.getProductMMID().concat(MMID_SUFFIX));
                if (Objects.isNull(vendor) || StringUtils.isNotEmpty(vendor.getError_message())) {
                    throw new CentralCommerceServiceException("Price Not Available", HttpStatus.BAD_REQUEST);
                }

                PricingMode pricingMode =
                        Optional.of(priceServiceResponse)
                                .flatMap(
                                        response ->
                                                response.getItems().stream()
                                                        .filter(priceResponse -> priceResponse.getIs_valid() &&
                                                                priceResponse.getMmId().equalsIgnoreCase(item.getProductMMID().concat(MMID_SUFFIX)) )
                                                        .findFirst())
                                .map(PriceResponse::getMaterialData)
                                .map(PricingMaterialData::getPricing_mode)
                                .orElse(null);

                Map<String, Object> freightChargeMap =
                        getFreightChargeDetails(vendor, pricingMode);

                boolean isFreightChargeAvailable =
                        (Boolean) freightChargeMap.get(IS_FREIGHT_CHARGE_AVAILABLE);
                double freightCharge = (Double) freightChargeMap.get(FREIGHT_CHARGE);

                if (!isFreightChargeAvailable) {
                    String error =
                            "FreightCharge is not available for product MMID: "
                                    + item.getProductMMID()
                                    + " & pinCode: "
                                    + priceRequest.getPinCode()
                                    + "with vendorLocationKey: "
                                    + vendor.getVendor().getVendor_location_key();
                    log.error(error);
                }
                log.info(
                        "freight charge for product MMID:{} is :{}",
                        item.getProductMMID(),
                        freightCharge);

                lineItemPrices.add(prepareLineItemPrice(vendor,productUomMap.get(item.getProductMMID()),pricingMode, item.getProductAttributes()));
            });

            return lineItemPrices;
        }catch (CentralCommerceServiceException ex){
            log.error(
                    "Pricing Service Exception occurred while making price call: {}",
                    ex.getMessage());
            return List.of(LineItemPrice.builder()
                    .success(false)
                    .displayErrorMessage(PRICE_FETCH_DISPLAY_ERROR_MESSAGE)
                    .errorMessage("Pricing Service Exception occurred while making price call: "+ex.getMessage())
                    .minMoq(0)
                    .build());
        }
        catch (Exception exception) {
            log.error(
                    "Exception occurred while making pdp price from pricing engine: {}",
                    exception.getMessage());
            return List.of(LineItemPrice.builder()
                    .success(false)
                    .displayErrorMessage(PRICE_FETCH_DISPLAY_ERROR_MESSAGE)
                    .errorMessage("Exception occurred while making pdp price from pricing engine: "+exception.getMessage())
                    .minMoq(0)
                    .build());
        }
    }

    private ProductPricingRequest getPricingProductRequest(
            PriceRequest priceRequest,
            Map<String, UomConvert> productUomMap) {

        return ProductPricingRequest.builder()
                .meta_data(
                        PricingRequestMetaData.builder()
                                .channel("portal")
                                .reference_id(PricingUtil.generateRandomText(16))
                                .build())
                .items(this.buildPricingProductVariantRequest(
                                        priceRequest,
                                        productUomMap,
                                        priceRequest.getItems()))
                .deliveryType("JOTS")
                .reference_id(PricingUtil.generateRandomText(16))
                .build();
    }

    public List<ProductVariantPricingRequest> buildPricingProductVariantRequest(
            PriceRequest priceRequest,
            Map<String, UomConvert> productUomMap,
            List<Item> items) {
        List<ProductVariantPricingRequest> productVariantPricingRequests = new ArrayList<>();
        items.forEach(item -> {
            Map<String, Object> variantMap = item.getProductAttributes().stream()
                    .collect(Collectors.toMap(
                            Attribute::getName,
                            Attribute::getValue
                    ));
            ProductVariantPricingRequest productVariantPricingRequest = ProductVariantPricingRequest.builder()
                    .mmid(item.getProductMMID().concat(MMID_SUFFIX))
                    .variant_attributes(variantMap)
                    .material_data(
                            PricingMaterialData.builder()
                                    .uom(productUomMap.get(item.getProductMMID()).getPrimaryUom().getUnit())
                                    .pricing_mode(null)
                                    .order_quantity(Double.parseDouble(productUomMap.get(item.getProductMMID()).getPrimaryUom().getValue().toString()))
                                    .ship_to_location(Integer.valueOf(priceRequest.getPinCode()))
                                    .reference_id(PricingUtil.generateRandomText(16))
                                    .build())
                    .priceItemMeta(
                            PriceItemMeta.builder()
                                    .customerId(PricingUtil.generateRandomText(16))
                                    .referenceId(PricingUtil.generateRandomText(16))
                                    .build())
                    .reference_id(PricingUtil.generateRandomText(16))
                    .build();
            productVariantPricingRequests.add(productVariantPricingRequest);
        });

        return productVariantPricingRequests;
    }

    public static Map<String, Object> getFreightChargeDetails(
            Vendor vendor, PricingMode pricingMode) {
        Map<String, Object> result = new HashMap<>();
        if(Objects.isNull(vendor)){
            throw new CentralCommerceServiceException("Price Not Available", HttpStatus.BAD_REQUEST);
        }
        String totalFreightPriceStr = vendor.getFreightPriceDetails().getTotalFreightPrice();
        boolean isFreightChargeAvailable = StringUtils.isNotEmpty(totalFreightPriceStr);
        double freightCharge = Optional.of(Double.parseDouble(Objects.nonNull(totalFreightPriceStr) ? totalFreightPriceStr : String.valueOf(0.0))).orElse(0.0);

        if (Objects.nonNull(pricingMode) && PricingMode.FOR.equals(pricingMode)) {
            isFreightChargeAvailable = true;
            freightCharge = 0.0;
        }

        result.put(IS_FREIGHT_CHARGE_AVAILABLE, isFreightChargeAvailable);
        result.put(FREIGHT_CHARGE, freightCharge);

        return result;
    }

    public LineItemPrice prepareLineItemPrice(
            Vendor vendor,
            UomConvert uomConvert,
            PricingMode pricingMode,
            List<Attribute> attributes){

        Double sellingPrice =
                Optional.ofNullable(vendor)
                        .map(v -> v.getPrice_details())
                        .flatMap(
                                details ->
                                        Optional.ofNullable(details.getCategory_selling_price())
                                                .map(String::valueOf)
                                                .map(Double::parseDouble))
                        .orElse(null);

        double primaryUomValue =
                PricingUtil.roundOffDouble(
                        Double.parseDouble(
                                Optional.of(uomConvert)
                                        .map(UomConvert::getPrimaryUom)
                                        .map(UomValueDetails::getValue)
                                        .map(String::valueOf)
                                        .orElse("0")),
                        4,
                        RoundingMode.HALF_EVEN);

        double displayVariantLineItemPrice = sellingPrice * primaryUomValue;

        return LineItemPrice.builder()
                .success(true)
                .errorMessage(
                        Objects.nonNull(sellingPrice) ? vendor.getError_message() : EMPTY_STRING)
                .displayErrorMessage(
                        Objects.nonNull(sellingPrice) ? vendor.getError_message() : EMPTY_STRING)
                .price(
                        LineItemPriceDto.builder()
                                .variantLineItemPrice(sellingPrice)
                                .primaryUomValue(String.valueOf(primaryUomValue))
                                .primaryUomLabel(uomConvert.getPrimaryUom().getLabel())
                                .displayPrimaryUomValue(
                                        String.valueOf(
                                                PricingUtil.roundOffDouble(
                                                        primaryUomValue, 3, RoundingMode.HALF_UP)))
                                .primaryUom(uomConvert.getPrimaryUom().getUnit())
                                .primaryUomVariantPrice(sellingPrice)
                                .displayPrimaryUomVariantPrice(
                                        Objects.nonNull(sellingPrice)
                                                ? PricingUtil.formatIndianCommaSeparated(
                                                sellingPrice.longValue())
                                                : null)
                                .displayVariantLineItemPrice(
                                        Objects.nonNull(displayVariantLineItemPrice)
                                                ? PricingUtil.formatIndianDoubleRupee(
                                                displayVariantLineItemPrice)
                                                : null)
                                .pricingMode(pricingMode)
                                .build())
                .data(null)
                .minMoq(0.0)
                .customAttributes(new HashSet<>(attributes))
                .build();
    }

    private Map<String, UomConvert> getProductUomMap(PriceRequest priceRequest, UomConvertResponse uomConvertResponse){
        Map<String, UomConvert> productUomMap = new HashMap<>();
        priceRequest.getItems().forEach(item -> {
            Map<String, UomConvert> filteredMap =
                    uomConvertResponse.getUomConvertedProducts().stream().filter(
                            uomConvert -> uomConvert.getProductMMID().equalsIgnoreCase(item.getProductMMID())).collect(Collectors.toMap(
                            UomConvert::getProductMMID,
                            u -> u));

            productUomMap.putAll(filteredMap);

        });

        return productUomMap;
    }

    private Map<String, Vendor> getPriceVendorMap(PricingServiceResponse priceServiceResponse){
        return priceServiceResponse.getItems()
                .stream()
                .filter(item -> Objects.nonNull(item.getVendors()))
                .collect(Collectors.toMap(
                        PriceResponse::getMmId,
                        priceResponse ->
                                priceResponse.getVendors().stream().findFirst().orElse(new Vendor())));
    }
}
