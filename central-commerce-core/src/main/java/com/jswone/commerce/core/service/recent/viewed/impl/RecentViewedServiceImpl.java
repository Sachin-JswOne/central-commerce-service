package com.jswone.commerce.core.service.recent.viewed.impl;

import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.BuyAgainResponse;
import com.jswone.commerce.core.model.PurchasedLineItemResponse;
import com.jswone.commerce.core.model.RecentViewedResponse;
import com.jswone.commerce.core.service.CartService;
import com.jswone.commerce.core.service.impl.BuyAgainServiceImplV2;
import com.jswone.commerce.core.service.recent.viewed.RecentViewedService;
import com.jswone.commons.enums.CartJourneyType;
import com.jswone.commons.enums.CartType;
import com.jswone.commons.graphql.v2.OrdersV2;
import com.jswone.commons.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecentViewedServiceImpl implements RecentViewedService {
    private final BuyAgainServiceImplV2 buyAgainServiceImplV2;
    private final CartService cartService;


    @Override
    public RecentViewedResponse recentViewedRemoval() {
        try {
            RecentViewedResponse recentViewedResponse = new RecentViewedResponse();
            Set<String> removeProducts = new HashSet<>();
            BuyAgainResponse buyAgainResponse = buyAgainServiceImplV2.getRecentPurchasedOrdersList(0, 50);
            if(Objects.nonNull(buyAgainResponse) && !buyAgainResponse.getVariantList().isEmpty()){
                Set<String> buyAgainProductMMID = buyAgainResponse.getVariantList().stream()
                                                   .map(PurchasedLineItemResponse::getProductMMID)
                                                   .collect(Collectors.toSet());
                if(!buyAgainProductMMID.isEmpty()) {
                    removeProducts.addAll(buyAgainProductMMID);
                }
            }
            String customerId = JwtTokenUtil.getUserIdForSession();
            CompletableFuture<OrdersV2> enquiryCartInfoCompletableFuture =
                    CompletableFuture.supplyAsync(
                            () ->
                                     cartService.getCartInfo(
                                            customerId,
                                            CartType.ENQUIRY_CART_V2,
                                            CartJourneyType.REQUIREMENTS_CART_V2));

            CompletableFuture<OrdersV2> orderCartInfoCompletableFuture =
                    CompletableFuture.supplyAsync(
                            () ->
                                    cartService.getCartInfo(
                                            customerId,
                                            CartType.ORDER,
                                            CartJourneyType.SALES_LEAD_CART));
            final OrdersV2 orderInfo = orderCartInfoCompletableFuture.join();
            final OrdersV2 enquiryCartInfo = enquiryCartInfoCompletableFuture.join();
            if(Objects.nonNull(orderInfo) && !orderInfo.getResults().isEmpty()) {
                Set<String> orderProductMMID = cartService.getProductMMID(orderInfo);
                if (!orderProductMMID.isEmpty()) {
                    removeProducts.addAll(orderProductMMID);
                }
            }
            if(Objects.nonNull(enquiryCartInfo) && !enquiryCartInfo.getResults().isEmpty()) {
                Set<String> cartProductMMID = cartService.getProductMMID(enquiryCartInfo);
                if (!cartProductMMID.isEmpty()) {
                    removeProducts.addAll(cartProductMMID);
                }
            }

            if (!removeProducts.isEmpty()) {
                recentViewedResponse.setRemoveProduct(removeProducts);
            }
            return recentViewedResponse;
        }catch (Exception e){
            throw new CentralCommerceServiceException(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
