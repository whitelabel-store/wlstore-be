package com.wlstore.pricing_service.outbox;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato do evento price.changed. Descreve o preço que passou a valer, em estado completo
 * (e não "o que mudou"): quem consome sempre sabe o valor atual sem depender de eventos anteriores.
 */
public record PriceChangedMessage(
        UUID tenantId,
        UUID productId,
        String sku,
        long amountCents,
        Long originalAmountCents,
        Instant validFrom
) {
}
