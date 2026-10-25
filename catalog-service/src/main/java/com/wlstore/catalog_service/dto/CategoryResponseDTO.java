package com.wlstore.catalog_service.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CategoryResponseDTO(
        UUID id,
        String title,
        String slug,
        List<CategoryResponseDTO> children,
        Instant createdAt,
        Instant updatedAt
) {
}
