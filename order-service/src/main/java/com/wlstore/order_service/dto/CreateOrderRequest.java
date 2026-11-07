package com.wlstore.order_service.dto;

import java.util.List;

public record CreateOrderRequest(
        List<OrderLineDTO> lines
) {
}
