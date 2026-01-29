package com.jswone.commerce.core.mapper;

import com.jswone.commerce.core.model.centralCatalogue.Product;
import com.jswone.commerce.core.model.centralCatalogue.ProductSlug;
import com.jswone.commerce.core.model.centralCatalogue.QuantityCard;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductSlugMapper {

    ProductSlug toProductSlug(Product product, List<QuantityCard> quantityCards);
}
