package com.jswone.commerce.core.mapper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jswone.commerce.core.entity.PurchasedSku;
import com.jswone.commerce.core.model.Uom;
import com.jswone.commerce.core.util.UomDeserializer;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

@Component
public class PurchasedSkuRowMapper implements RowMapper<PurchasedSku> {

    private final Gson gson;

    public PurchasedSkuRowMapper(Gson gson) {
        this.gson =
                new GsonBuilder().registerTypeAdapter(Uom.class, new UomDeserializer()).create();
    }

    @Override
    public PurchasedSku mapRow(ResultSet rs, int rowNum) throws SQLException {

        PurchasedSku purchasedSku = new PurchasedSku();
        purchasedSku.setCustomerId(rs.getString("customer_id"));
        purchasedSku.setCustomerEmail(rs.getString("customer_email"));
        purchasedSku.setOrderPlacedDate(rs.getDate("order_placed_date"));
        purchasedSku.setInvoiceNo(rs.getString("invoice_no"));
        purchasedSku.setInvoiceDate(rs.getDate("invoice_date"));
        purchasedSku.setProductName(rs.getString("product_name"));
        purchasedSku.setProductKey(rs.getString("product_key"));
        purchasedSku.setProductSlug(rs.getString("product_slug"));
        purchasedSku.setVariantName(rs.getString("variant_name"));
        purchasedSku.setVariantKey(rs.getString("variant_key"));
        purchasedSku.setSkuAttributes(gson.fromJson(rs.getString("sku_attributes"), Map.class));
        purchasedSku.setCtSkuAttributes(
                gson.fromJson(rs.getString("ct_sku_attributes"), Map.class));
        purchasedSku.setCtUom(gson.fromJson(rs.getString("ctuom"), Uom.class));
        purchasedSku.setPrimaryQuantity(gson.fromJson(rs.getString("primary_quantity"), Uom.class));
        purchasedSku.setSecondaryQuantity(
                gson.fromJson(rs.getString("secondary_quantity"), Uom.class));
        purchasedSku.setCtAttributeHash(rs.getString("ct_attribute_hash"));
        String productMmid = "";  // Default value
        try {
            productMmid = rs.getString("product_mmid");
        } catch (SQLException e) {
            productMmid = "";
        }
        purchasedSku.setProductMMID(productMmid);

        String variantMmid = "";  // Default value
        try {
            variantMmid = rs.getString("variant_mmid");
        } catch (SQLException e) {
            variantMmid = "";
        }
        purchasedSku.setVariantMMID(variantMmid);

        return purchasedSku;
    }
}

