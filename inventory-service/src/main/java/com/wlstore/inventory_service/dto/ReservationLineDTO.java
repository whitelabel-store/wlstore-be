package com.wlstore.inventory_service.dto;

public record ReservationLineDTO(
        String sku,
        int quantity
) {
}
