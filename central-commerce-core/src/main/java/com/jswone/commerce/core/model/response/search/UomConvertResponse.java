package com.jswone.commerce.core.model.response.search;

import com.jswone.commerce.core.model.UomConvert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UomConvertResponse {

    List<UomConvert> uomConvertedProducts;
}
