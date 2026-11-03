package com.wlstore.inventory_service.dto;

import com.wlstore.inventory_service.model.Stock;

public record StockResponseDTO(
        String sku,
        int quantityOnHand,
        int quantityReserved,
        int available
) {
    public static StockResponseDTO from(Stock stock) {
        return new StockResponseDTO(
                stock.getSku(),
                stock.getQuantityOnHand(),
                stock.getQuantityReserved(),
                stock.available()
        );
    }
}
