package com.wlstore.catalog_service.mapper;

import com.wlstore.catalog_service.dto.*;
import com.wlstore.catalog_service.model.Category;
import com.wlstore.catalog_service.model.Product;
import com.wlstore.catalog_service.model.ProductImage;
import com.wlstore.catalog_service.model.ProductVariant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Product toEntity(ProductRequestDTO requestDTO);

    // the prices do not live on Product: the service fills them from the price read model
    @Mapping(target = "priceFromCents", ignore = true)
    @Mapping(target = "originalPriceFromCents", ignore = true)
    ProductResponseDTO toResponseDTO(Product product);

    ProductVariantResponseDTO toVariantResponse(ProductVariant variant);

    @Mapping(target = "variantSku", source = "variant.sku")
    ProductImageResponseDTO toImageResponse(ProductImage image);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    ProductVariant toVariantEntity(ProductVariantRequestDTO variantRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "variant", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    ProductImage toImageEntity(ProductImageRequestDTO imageRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateProductFromRequest(ProductRequestDTO productRequest, @MappingTarget Product product);

    default CategorySummaryDTO toCategorySummary(Category category) {
        if (category == null) {
            return null;
        }

        Deque<String> path = new ArrayDeque<>();
        for (Category current = category; current != null; current = current.getParent()) {
            path.addFirst(current.getTitle());
        }

        return new CategorySummaryDTO(category.getId(), category.getTitle(), category.getSlug(), List.copyOf(path));
    }

}