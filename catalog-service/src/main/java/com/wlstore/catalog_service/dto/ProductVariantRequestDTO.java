package com.wlstore.catalog_service.dto;

import com.wlstore.catalog_service.enums.ActiveStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record ProductVariantRequestDTO(
        @NotBlank
        @Size(max = 64)
        String sku,

        @NotBlank
        @Size(max = 255)
        String name,

        Map<String, String> optionValues,

        @PositiveOrZero
        Integer weightGrams,

        ActiveStatus status,

        @PositiveOrZero
        Integer position
) {

}
