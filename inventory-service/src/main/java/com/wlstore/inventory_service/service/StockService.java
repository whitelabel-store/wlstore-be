package com.wlstore.inventory_service.service;

public interface StockService {

    /**
     * Reserves units of a SKU for the current tenant.
     *
     * @throws IllegalArgumentException   when quantity is not positive (400)
     * @throws com.wlstore.common.exception.ResourceNotFoundException when the SKU has no stock row (404)
     * @throws com.wlstore.inventory_service.exception.InsufficientStockException when there is not enough available (409)
     */
    void reserve(String sku, int quantity);
}
