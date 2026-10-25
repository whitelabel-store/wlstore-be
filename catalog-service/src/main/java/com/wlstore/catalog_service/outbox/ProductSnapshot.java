package com.wlstore.catalog_service.outbox;

import com.wlstore.catalog_service.model.Product;

import java.util.List;
import java.util.UUID;

/**
 * Contrato do evento publicado pelo catálogo. Só carrega o que pricing/inventory precisam:
 * quem é o produto, em qual tenant, se está ativo e quais SKUs existem.
 */
public record ProductSnapshot(
        UUID productId,
        UUID tenantId,
        String status,
        List<VariantSnapshot> variants
) {
    public record VariantSnapshot(String sku, String status) {}

    public static ProductSnapshot from(Product product) {
        List<VariantSnapshot> variants = product
                .getVariants()
                .stream()
                .map(v -> new VariantSnapshot(v.getSku(), v.getStatus().name()))
                .toList();

        return new ProductSnapshot(
                product.getId(),
                product.getTenantId(),
                product.getStatus().name(),
                variants
        );
    }
}
