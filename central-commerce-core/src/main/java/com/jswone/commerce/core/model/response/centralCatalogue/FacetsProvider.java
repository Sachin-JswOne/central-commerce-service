package com.jswone.commerce.core.model.response.centralCatalogue;

import java.util.Map;
import java.util.Set;

public interface FacetsProvider {
    Map<String, Set<String>> getFacets();
}
