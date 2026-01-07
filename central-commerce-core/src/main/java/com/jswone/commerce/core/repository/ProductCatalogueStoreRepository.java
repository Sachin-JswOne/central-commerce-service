package com.jswone.commerce.core.repository;

import com.google.cloud.datastore.Key;
import com.google.cloud.spring.data.datastore.repository.DatastoreRepository;
import com.google.cloud.spring.data.datastore.repository.query.Query;
import com.jswone.commerce.core.entity.catalogue.ProductCatalogueStore;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductCatalogueStoreRepository extends DatastoreRepository<ProductCatalogueStore, Key> {

    @Query("SELECT * FROM product_catalogue_store WHERE productMaterialMasterId IN @productMaterialMasterIds")
    List<ProductCatalogueStore> findProductCatalogueStoresByProductMaterialMasterIds(@Param("productMaterialMasterIds") List<String> productMaterialMasterIds);

    ProductCatalogueStore findProductCatalogueStoresByProductMaterialMasterId(String productMaterialMasterId);

}
