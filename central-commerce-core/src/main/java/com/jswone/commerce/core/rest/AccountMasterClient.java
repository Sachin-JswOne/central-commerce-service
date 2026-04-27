package com.jswone.commerce.core.rest;

import com.jswone.commerce.core.model.auth.AccountMasterResponse;
import com.jswone.commerce.core.model.auth.PageResponseDTO;
import com.jswone.commerce.core.model.auth.ResourceResponse;

import java.util.Map;

public interface AccountMasterClient {
    Map<String,String> getAdminPermissionMap();
    PageResponseDTO<ResourceResponse> getAllResources();
}
