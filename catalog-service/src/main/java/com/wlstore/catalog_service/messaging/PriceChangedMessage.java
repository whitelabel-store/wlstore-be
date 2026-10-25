package com.wlstore.catalog_service.messaging;

import java.time.Instant;
import java.util.UUID;

public record PriceChangedMessage(
        UUID tenantId,
        UUID productId,
        String sku,
        long amountCents,
        Long originalAmountCents,
        Instant validFrom
) {
}
