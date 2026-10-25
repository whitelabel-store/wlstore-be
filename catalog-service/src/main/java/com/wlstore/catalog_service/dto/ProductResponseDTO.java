package com.wlstore.catalog_service.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductResponseDTO(
        UUID id,
        String title,
        String brand,
        String description,
        String status,
        CategorySummaryDTO category,
        List<ProductVariantResponseDTO> variants,
        List<ProductImageResponseDTO> images,
        Long priceFromCents,
        Long originalPriceFromCents,
        Instant createdAt,
        Instant updatedAt
) {
}