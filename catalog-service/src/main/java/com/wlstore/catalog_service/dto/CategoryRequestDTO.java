package com.wlstore.catalog_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record CategoryRequestDTO(
        @Schema(description = "Only used on update, to match an existing subcategory. Ignored on create.")
        UUID id,

        @NotBlank(message = "Title is required")
        String title,

        @Schema(description = "URL-friendly identifier. Derived from the title when omitted on create, "
                + "and left untouched by a rename — send it explicitly to change it. "
                + "Collisions get a numeric suffix (capas-de-silicone-2).",
                example = "capas-de-silicone")
        String slug,

        List<@Valid CategoryRequestDTO> children
) {

}
