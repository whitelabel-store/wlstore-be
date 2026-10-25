package com.wlstore.catalog_service.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CategoryRefDTO(
        @NotNull(message = "Category id is required")
        UUID id
) {
}
