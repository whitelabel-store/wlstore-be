package com.wlstore.inventory_service.exception;

import lombok.Getter;

@Getter
public class InsufficientStockException extends RuntimeException {

    private final String sku;
    private final int requested;
    private final int available;

    public InsufficientStockException(String sku, int requested, int available) {
        super("Insufficient stock for SKU %s: requested %d, available %d".formatted(sku, requested, available));
        this.sku = sku;
        this.requested = requested;
        this.available = available;
    }

}