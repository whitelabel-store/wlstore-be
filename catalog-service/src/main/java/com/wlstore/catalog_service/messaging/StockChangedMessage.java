package com.wlstore.catalog_service.messaging;

import java.util.UUID;

/**
 * What the catalog needs from the inventory's stock.changed event. The event carries more
 * (on hand, reserved); fields not listed here are ignored when reading it.
 */
public record StockChangedMessage(
        UUID tenantId,
        String sku,
        int available,
        long version
) {

}
