package com.jswone.commerce.core.service.mou;

import com.fasterxml.jackson.databind.JsonNode;

public interface IncentiveService {
    JsonNode getIncentives(String mouId, String financialYear, String productCategory, String mouType);
}