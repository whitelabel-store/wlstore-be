package com.wlstore.order_service.dto;

public record OrderLineDTO(
        String sku,
        int quantity
) {
}
