package com.jswone.commerce.core.service;

import com.jswone.commerce.core.model.request.AiInvokeRequest;
import com.jswone.commerce.core.model.response.AiInvokeResponse;

public interface AiService {

    AiInvokeResponse invoke(AiInvokeRequest request);
}
