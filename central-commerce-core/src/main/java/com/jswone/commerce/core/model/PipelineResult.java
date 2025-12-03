package com.jswone.commerce.core.model;

import com.jswone.commerce.core.entity.PurchasedSku;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PipelineResult {

    public List<String> successCustomerIds = new ArrayList<>();

    public List<Map.Entry<String, List<PurchasedSku>>> failedEntries = new ArrayList<>();
}