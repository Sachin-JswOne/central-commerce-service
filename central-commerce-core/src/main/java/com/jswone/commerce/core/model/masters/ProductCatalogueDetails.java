package com.jswone.commerce.core.model.masters;

import com.jswone.commerce.core.model.Uom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductCatalogueDetails {
    private String productMMId;
    private String brand;
    private String subBrand;
    private String grade;
    private String subGrade;
    private String productName;
    private List<Uom> uom;
    private String form;
    private Category category;
}
