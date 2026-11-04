package com.wlstore.inventory_service.outbox;

import java.util.UUID;

/**
 * Contract of the stock.changed event. It describes the balance of a SKU as it is after the change, in full
 * (not "what changed"): consumers can simply replace what they have, and use {@code version} to discard
 * events older than the one they already applied.
 */
public record StockChangedMessage(
        UUID tenantId,
        String sku,
        int quantityOnHand,
        int quantityReserved,
        int available,
        long version
) {
}
