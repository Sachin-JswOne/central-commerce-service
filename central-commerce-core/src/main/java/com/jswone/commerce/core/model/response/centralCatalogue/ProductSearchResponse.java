package com.jswone.commerce.core.model.response.centralCatalogue;

import com.jswone.commerce.core.model.centralCatalogue.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchResponse {
    private List<Product> products;
}
