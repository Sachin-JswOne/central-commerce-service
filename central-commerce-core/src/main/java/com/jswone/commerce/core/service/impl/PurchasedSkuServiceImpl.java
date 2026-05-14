package com.jswone.commerce.core.service.impl;

import com.jswone.commerce.core.entity.PurchasedSku;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.mapper.PurchasedSkuRowMapper;
import com.jswone.commerce.core.service.PurchasedSkuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PurchasedSkuServiceImpl implements PurchasedSkuService {

    private final JdbcTemplate jdbcTemplate;

    private final PurchasedSkuRowMapper purchasedSkuRowMapper;

    public PurchasedSkuServiceImpl(
            @Qualifier("historicalJdbcTemplate") JdbcTemplate jdbcTemplate, PurchasedSkuRowMapper purchasedSkuRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.purchasedSkuRowMapper = purchasedSkuRowMapper;
    }

    @Override
    public List<PurchasedSku> fetchRecentlyPurchasedSku(String customerId) {
        final String sql = "SELECT * FROM public.recent_purchase_v2_vw where customer_id = ?";
        return jdbcTemplate.query(sql, purchasedSkuRowMapper, customerId);
    }

    @Override
    public List<PurchasedSku> fetchRecentlyPurchasedSkuForAllCustomers(int offset, int limit) {
        final String sql = """
                    SELECT *
                    FROM public.recent_purchase_v2_vw
                    ORDER BY customer_id
                    LIMIT ? OFFSET ?
                """;

        return jdbcTemplate.query(sql, purchasedSkuRowMapper, limit, offset);
    }

    @Override
    public Boolean checkTransactingCustomer(String customerId) {
        try {
            final String sql = """
                    SELECT EXISTS (
                    SELECT 1
                    FROM public.recent_purchase_v2_vw
                    WHERE customer_id = ?
                    )
                    """;

            return jdbcTemplate.queryForObject(sql, Boolean.class, customerId);

        } catch (Exception ex) {
            log.error("Error while checking transacting customer for customerId: {}", customerId, ex);
            throw new CentralCommerceServiceException("Failed to check transacting customer for customerId: " + customerId, ex);
        }
    }
}
