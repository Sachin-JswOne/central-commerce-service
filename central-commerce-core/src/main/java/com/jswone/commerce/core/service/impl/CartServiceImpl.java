package com.jswone.commerce.core.service.impl;

import com.commercetools.api.client.ByProjectKeyRequestBuilder;
import com.commercetools.api.models.graph_ql.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jswone.commerce.core.service.CartService;
import com.jswone.commerce.core.util.CartUtil;
import com.jswone.commons.enums.CartJourneyType;
import com.jswone.commons.enums.CartType;
import com.jswone.commons.graphql.v2.OrdersV2;
import com.jswone.commons.graphql.v2.ResultV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.jswone.commerce.core.constants.GenericConstants.*;
import static com.jswone.commons.constants.ContentfulConstants.QUERY_CONDITION;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final ObjectMapper objectMapper;
    private final ByProjectKeyRequestBuilder requestBuilder;

    @Override
    public OrdersV2 getCartInfo(
            String customerId, CartType cartType, CartJourneyType cartJourneyType) {
        final GraphQLResponse orderGraphQLResponse =
                this.getCartsResponse(customerId, cartType, cartJourneyType);
        return getCartsResponseFromGraphQL(orderGraphQLResponse);
    }

    private GraphQLResponse getCartsResponse(
            String customerId, CartType cartType, CartJourneyType cartJourneyType) {

        GraphQLVariablesMap graphMap =
                GraphQLVariablesMapBuilder.of()
                        .addValue(
                                QUERY_CONDITION,
                                String.format(
                                        CARTS_DATA_FROM_CUSTOMER_ID,
                                        customerId,
                                        cartType.getValue(),
                                        cartJourneyType.name()))
                        .build();
        return this.getCartResponse(graphMap);
    }

    private GraphQLResponse getCartResponse(GraphQLVariablesMap graphMap) {

        GraphQLRequest request =
                GraphQLRequestBuilder.of()
                        .query(GRAPHQL_QUERY_FOR_CARTS_FROM_CUSTOMER_ID)
                        .variables(graphMap)
                        .build();

        return requestBuilder.graphql().post(request).executeBlocking().getBody();
    }

    private OrdersV2 getCartsResponseFromGraphQL(GraphQLResponse graphQLResponse) {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper.convertValue(
                Optional.of(graphQLResponse)
                        .map(GraphQLResponse::getData)
                        .map(Map.class::cast)
                        .map(graphqlResult -> graphqlResult.get(CARTS))
                        .orElse(null),
                OrdersV2.class);
    }
}
