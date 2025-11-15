package com.jswone.commerce.core.repository;

import com.google.cloud.datastore.Key;
import com.google.cloud.spring.data.datastore.repository.DatastoreRepository;
import com.jswone.commerce.core.entity.catalogue.ProductCatalogueStore;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductCatalogueStoreRepository
        extends DatastoreRepository<ProductCatalogueStore, Key>{
    ProductCatalogueStore findProductCatalogueStoresByProductKey(String productKey);

    ProductCatalogueStore findProductCatalogueStoresByProductMaterialMasterId(String productMaterialMasterId);

}
