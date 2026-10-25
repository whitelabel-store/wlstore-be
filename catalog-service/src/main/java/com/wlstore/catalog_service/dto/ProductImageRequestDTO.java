package com.wlstore.catalog_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductImageRequestDTO(
        @NotBlank
        @Size(max = 2048)
        String url,

        @Size(max = 255)
        String altText,

        @PositiveOrZero
        Integer position,

        boolean primaryImage,

        String variantSku
) {
}
