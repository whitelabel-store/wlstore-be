package com.wlstore.inventory_service.exception;

import lombok.Getter;

@Getter
public class StockBelowReservedException extends RuntimeException {

    private final String sku;
    private final int requestedOnHand;
    private final int reserved;

    public StockBelowReservedException(String sku, int requestedOnHand, int reserved) {
        super("Cannot set on hand of SKU %s to %d: %d units are already reserved"
                .formatted(sku, requestedOnHand, reserved));
        this.sku = sku;
        this.requestedOnHand = requestedOnHand;
        this.reserved = reserved;
    }
}