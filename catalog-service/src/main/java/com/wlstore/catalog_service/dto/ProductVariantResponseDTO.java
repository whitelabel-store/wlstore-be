package com.wlstore.catalog_service.dto;

import com.wlstore.catalog_service.enums.ActiveStatus;

import java.util.Map;
import java.util.UUID;

public record ProductVariantResponseDTO(
        UUID id,
        String sku,
        String name,
        Map<String, String> optionValues,
        Integer weightGrams,
        ActiveStatus status,
        Integer position
) {
}
