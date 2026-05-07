package com.jswone.commerce.core.service.recent.viewed.impl;

import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.BuyAgainResponse;
import com.jswone.commerce.core.model.PurchasedLineItemResponse;
import com.jswone.commerce.core.model.RecentViewedResponse;
import com.jswone.commerce.core.service.CartService;
import com.jswone.commerce.core.service.impl.BuyAgainServiceImplV2;
import com.jswone.commerce.core.service.recent.viewed.RecentViewedService;
import com.jswone.commerce.core.util.CartUtil;
import com.jswone.commons.enums.CartJourneyType;
import com.jswone.commons.enums.CartType;
import com.jswone.commons.graphql.v2.OrdersV2;
import com.jswone.commons.graphql.v2.ResultV2;
import com.jswone.commons.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.jswone.commerce.core.constants.GenericConstants.PRD_MATERIAL_MASTER_ID;

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
            getRemoveProductsForBuyAgain(buyAgainResponse, removeProducts);

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
            getRemoveProductsForCart(orderInfo, enquiryCartInfo, removeProducts);
            if (!removeProducts.isEmpty()) {
                recentViewedResponse.setRemoveProduct(removeProducts);
            }
            return recentViewedResponse;
        }catch (Exception e){
            throw new CentralCommerceServiceException(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    private void getRemoveProductsForBuyAgain(BuyAgainResponse buyAgainResponse, Set<String> removeProducts){
        if(Objects.nonNull(buyAgainResponse) && !buyAgainResponse.getVariantList().isEmpty()){
            Set<String> buyAgainProductMMID = buyAgainResponse.getVariantList().stream()
                    .map(PurchasedLineItemResponse::getProductMMID)
                    .collect(Collectors.toSet());
            if(!buyAgainProductMMID.isEmpty()) {
                removeProducts.addAll(buyAgainProductMMID);
            }
        }
    }

    private void getRemoveProductsForCart(OrdersV2 orderInfo, OrdersV2 enquiryCartInfo, Set<String> removeProducts){
        if(Objects.nonNull(orderInfo) && !orderInfo.getResults().isEmpty()) {
            Set<String> orderProductMMID = getProductMMID(orderInfo);
            if (Objects.nonNull(orderProductMMID) && !orderProductMMID.isEmpty()) {
                removeProducts.addAll(orderProductMMID);
            }
        }else{
            if(Objects.nonNull(enquiryCartInfo) && !enquiryCartInfo.getResults().isEmpty()) {
                Set<String> cartProductMMID = getProductMMID(enquiryCartInfo);
                if (Objects.nonNull(cartProductMMID) && !cartProductMMID.isEmpty()) {
                    removeProducts.addAll(cartProductMMID);
                }
            }
        }
    }

    private Set<String> getProductMMID(OrdersV2 cart){
        return CartUtil.getProductMMID(cart, PRD_MATERIAL_MASTER_ID);
    }
}
