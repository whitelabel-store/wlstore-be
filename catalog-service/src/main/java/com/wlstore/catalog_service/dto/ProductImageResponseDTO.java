package com.wlstore.catalog_service.dto;

import java.util.UUID;

public record ProductImageResponseDTO(
        UUID id,
        String url,
        String altText,
        Integer position,
        boolean primaryImage,
        String variantSku
) {
}
