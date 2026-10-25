package com.wlstore.catalog_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

public record CategorySummaryDTO(
        UUID id,
        String title,
        String slug,

        @Schema(description = "Breadcrumb from the root category down to this one",
                example = "[\"Acessorios\", \"Acessorios para Celular\", \"Capas e Cases\", \"Capas de Silicone\"]")
        List<String> path
) {
}
