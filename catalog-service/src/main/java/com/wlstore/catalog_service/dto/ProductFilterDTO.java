package com.wlstore.catalog_service.dto;

import com.wlstore.catalog_service.enums.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record ProductFilterDTO(
        @Schema(description = "Partial, case-insensitive match on the product title", example = "redmi")
        String title,

        @Schema(description = "Partial, case-insensitive match on the brand", example = "xiaomi")
        String brand,

        @Schema(description = "Exact status match", example = "ACTIVE")
        ProductStatus status,

        @Schema(description = "Filters by category and, by default, everything below it in the tree",
                example = "ff70bf22-e67a-4cc7-aee6-ed7246955d5a")
        UUID categoryId,

        @Schema(description = "Same as categoryId, but addressed by slug — for storefront URLs. "
                + "Ignored when categoryId is also sent.",
                example = "capas-e-cases")
        String categorySlug,

        @Schema(description = "When false, matches only the exact categoryId instead of its whole subtree",
                defaultValue = "true")
        Boolean includeChildren,

        @Schema(description = "Keeps products whose \"from\" price (lowest variant price) is at least this, in cents. "
                + "Products without any price never match a price filter.",
                example = "10000")
        Long minPriceCents,

        @Schema(description = "Keeps products whose \"from\" price (lowest variant price) is at most this, in cents. "
                + "Products without any price never match a price filter.",
                example = "150000")
        Long maxPriceCents
) {
    public boolean shouldIncludeSubchildren() {
        return includeChildren == null || includeChildren;
    }
}