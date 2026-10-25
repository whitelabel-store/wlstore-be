package com.wlstore.catalog_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ProductRequestDTO(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Brand is required")
        String brand,

        @NotBlank(message = "Description is required")
        String description,

        @Schema(description = "Catalog lifecycle of the product", example = "ACTIVE",
                allowableValues = {"DRAFT", "ACTIVE", "ARCHIVED"})
        @NotBlank(message = "Status is required")
        String status,

        @NotNull(message = "Category is required")
        @Valid
        CategoryRefDTO category,

        @NotEmpty(message = "At least one variant is required")
        @Valid
        List<ProductVariantRequestDTO> variants,

        @Valid
        List<ProductImageRequestDTO> images
) {
}