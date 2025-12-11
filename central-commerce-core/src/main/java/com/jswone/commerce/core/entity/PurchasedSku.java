package com.jswone.commerce.core.entity;

import com.jswone.commerce.core.model.Uom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PurchasedSku {
    private String customerId;
    private Date orderPlacedDate;
    private String invoiceNo;
    private String customerEmail;
    private Date invoiceDate;
    private String productName;
    private String productKey;
    private String variantName;
    private Map<String, String> skuAttributes;
    private Uom primaryQuantity;
    private String productSlug;
    private Uom secondaryQuantity;
    private String variantKey;
    private Map<String, String> ctSkuAttributes;
    private Uom ctUom;
    private String ctAttributeHash;
    private String productMMID;
    private String variantMMID;
}
