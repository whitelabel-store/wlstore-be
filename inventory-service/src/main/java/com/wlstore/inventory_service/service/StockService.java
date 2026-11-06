package com.wlstore.inventory_service.service;

import com.wlstore.inventory_service.dto.ReservationLineDTO;
import com.wlstore.inventory_service.dto.StockResponseDTO;

import java.util.List;
import java.util.UUID;

public interface StockService {

    /**
     * Reserves units of a SKU for the current tenant.
     *
     * @throws IllegalArgumentException   when quantity is not positive (400)
     * @throws com.wlstore.common.exception.ResourceNotFoundException when the SKU has no stock row (404)
     * @throws com.wlstore.inventory_service.exception.InsufficientStockException when there is not enough available (409)
     */
    void reserve(String sku, int quantity);

    /**
     * Reserves every line of an order in ONE transaction: all of them or none of them.
     *
     * @throws com.wlstore.inventory_service.exception.InsufficientStockException when some line lacks stock (409)
     * @throws com.wlstore.common.exception.ResourceNotFoundException when some SKU has no stock row (404)
     */
    void reserveAll(UUID orderId, List<ReservationLineDTO> lines);

    /**
     * @throws com.wlstore.common.exception.ResourceNotFoundException when the SKU has no stock row (404)
     */
    StockResponseDTO get(String sku);

    /**
     * Replaces the physical balance of a SKU. Reservations are kept.
     *
     * @throws IllegalArgumentException   when the quantity is negative (400)
     * @throws com.wlstore.common.exception.ResourceNotFoundException when the SKU has no stock row (404)
     * @throws com.wlstore.inventory_service.exception.StockBelowReservedException when it would drop below the reserved units (409)
     */
    StockResponseDTO setOnHand(String sku, int quantityOnHand);

}
