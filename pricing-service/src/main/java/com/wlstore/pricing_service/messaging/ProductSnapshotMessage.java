package com.wlstore.pricing_service.messaging;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record ProductSnapshotMessage(
        UUID productId,
        UUID tenantId,
        String status,
        List<VariantMessage> variants
) {
    public Set<String> skus() {
        if (variants == null) {
            return Set.of();
        }

        return variants
                .stream()
                .map(VariantMessage::sku)
                .collect(Collectors.toSet());
    }

    public record VariantMessage(String sku, String status) {
    }

}
