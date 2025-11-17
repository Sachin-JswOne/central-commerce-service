package com.jswone.commerce.core.model.centralCatalogue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributes {
    private String product_id;
    private String grade;
    private String created_at;
    private Integer id;
    private String created_by;
    private String brand;
    private String product_title;
    private String sub_grade;
    private Double diameter_min;

    private List<String> text_set;
    private String localised_text;
    private String meta_title;
    private List<Integer> number_set;
    private String localized_date;
    private Boolean boolean_attribute;
    private Integer number;
    private String meta_description;
    private Integer Diametermm;
    private String rich_text;
    private String text;
    private String slug;
}
