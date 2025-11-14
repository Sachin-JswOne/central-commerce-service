package com.jswone.commerce.core.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.jswone.commerce.core.entity.productCatalogueStore.ProductCatalogueStore;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Repository
public class ProductCatalogueStoreRepository {
    private final Firestore firestore;

    public ProductCatalogueStoreRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    private CollectionReference collection() {
        return firestore.collection("product_catalogue_store");
    }

    private Set<ProductCatalogueStore> executeQuery(Query query) throws Exception {
        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> docs = future.get().getDocuments();
        return docs.stream().map(doc -> doc.toObject(ProductCatalogueStore.class)).collect(Collectors.toSet());
    }

    public List<ProductCatalogueStore> findProductCatalogueStoresByProductKey(List<String> productKeys) throws Exception {
        int batchSize = 10;
        if (productKeys.isEmpty()) return Collections.emptyList();

        return IntStream.iterate(0, i -> i + batchSize)
                .limit((productKeys.size() + batchSize - 1) / batchSize) // total batches
                .mapToObj(i -> productKeys.subList(i, Math.min(i + batchSize, productKeys.size())))
                .flatMap(batch -> {
                    try {
                        return executeQuery(collection().whereIn("productKey", batch)).stream();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    public Set<ProductCatalogueStore> findProductEntriesForBrandAndCatalogueEnabledFilter(
            String categoryId,
            List<String> brandValue,
            List<String> catalogueEnabledFor,
            boolean portalEnabled) throws Exception {

        Set<ProductCatalogueStore> result = new HashSet<>();
        for (String brand : brandValue) {
            for (String catalogue : catalogueEnabledFor) {
                Query query = collection()
                        .whereEqualTo("categoryIds", categoryId)
                        .whereEqualTo("brandValue", brand)
                        .whereEqualTo("catalogueEnabledFor", catalogue)
                        .whereEqualTo("portalEnabled", portalEnabled);
                result.addAll(executeQuery(query));
            }
        }
        return result;
    }

    public Set<ProductCatalogueStore> findProductEntriesForBrandFilter(
            String categoryId,
            List<String> brandValue,
            boolean portalEnabled) throws Exception {

        Set<ProductCatalogueStore> result = new HashSet<>();
        for (String brand : brandValue) {
            Query query = collection()
                    .whereEqualTo("categoryIds", categoryId)
                    .whereEqualTo("brandValue", brand)
                    .whereEqualTo("portalEnabled", portalEnabled);
            result.addAll(executeQuery(query));
        }
        return result;
    }

    public Set<ProductCatalogueStore> findProductEntriesForGradeFilter(
            String categoryId,
            List<String> grade,
            boolean portalEnabled) throws Exception {

        Set<ProductCatalogueStore> result = new HashSet<>();
        for (String g : grade) {
            Query query = collection()
                    .whereEqualTo("categoryIds", categoryId)
                    .whereEqualTo("grade", g)
                    .whereEqualTo("portalEnabled", portalEnabled);
            result.addAll(executeQuery(query));
        }
        return result;
    }

    public Set<ProductCatalogueStore> findProductEntriesForCategoryFilter(
            String categoryId,
            boolean portalEnabled,
            String catalogueEnabledFor) throws Exception {

        Query query = collection()
                .whereEqualTo("categoryIds", categoryId)
                .whereEqualTo("portalEnabled", portalEnabled)
                .whereEqualTo("catalogueEnabledFor", catalogueEnabledFor);

        return executeQuery(query);
    }

    public Set<ProductCatalogueStore> findProductEntriesForCategoryAndPortalEnabledFilter(
            String categoryId,
            boolean portalEnabled) throws Exception {

        Query query = collection()
                .whereEqualTo("categoryIds", categoryId)
                .whereEqualTo("portalEnabled", portalEnabled);

        return executeQuery(query);
    }

    public ProductCatalogueStore findProductCatalogueStoresByProductSlug(String productSlug) throws Exception {
        Query query = collection().whereEqualTo("productSlug", productSlug);
        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> docs = future.get().getDocuments();

        if (docs.isEmpty()) return null;
        return docs.get(0).toObject(ProductCatalogueStore.class);
    }

    public ProductCatalogueStore findProductCatalogueStoresByProductKey(String productKey) throws Exception {
        Query query = collection().whereEqualTo("productKey", productKey);
        ApiFuture<QuerySnapshot> future = query.get();
        QuerySnapshot snapshot = future.get();

        if (snapshot.isEmpty()) {
            return null; // or throw an exception
        }

        return snapshot.getDocuments().get(0).toObject(ProductCatalogueStore.class);
    }
}
