package com.jswone.commerce.core.model.centralCatalogue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssociatedCategory {
    private String path;
    private String rank;
    private AssociatedCategoryAttributes attributes;
    private String id;
    private List<Traversal> traversal;
}
