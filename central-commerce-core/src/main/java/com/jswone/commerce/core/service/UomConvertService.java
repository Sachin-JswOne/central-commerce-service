package com.jswone.commerce.core.service;

import com.jswone.commerce.core.model.response.UomConvertResponse;
import com.jswone.commerce.core.model.request.UomConvertRequest;

import java.util.List;

public interface UomConvertService {

    List<UomConvertResponse> convertUom(List<UomConvertRequest> uomConvertRequest);
}
