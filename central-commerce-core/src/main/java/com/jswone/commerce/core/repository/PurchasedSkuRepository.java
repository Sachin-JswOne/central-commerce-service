package com.jswone.commerce.core.repository;

import com.jswone.commerce.core.entity.PurchasedSku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchasedSkuRepository extends JpaRepository<PurchasedSku, String> {

    List<PurchasedSku> findByCustomerId(String customerId);
}
