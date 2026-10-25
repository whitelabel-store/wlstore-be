package com.wlstore.pricing_service.dto;

import com.wlstore.pricing_service.model.Price;

import java.time.Instant;

public record PriceResponseDTO(
        String sku,
        long amountCents,
        Long originalAmountCents,
        Instant validFrom,
        Instant validTo
) {
    public static PriceResponseDTO from (Price price) {
        return new PriceResponseDTO(
                price.getSku(),
                price.getAmountCents(),
                price.getOriginalAmountCents(),
                price.getValidFrom(),
                price.getValidTo()
        );
    }
}
