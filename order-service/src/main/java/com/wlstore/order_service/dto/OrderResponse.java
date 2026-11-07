package com.wlstore.order_service.dto;

import com.wlstore.order_service.enums.OrderStatus;
import com.wlstore.order_service.model.Order;

import java.util.UUID;

public record OrderResponse(
        UUID id,
        OrderStatus status
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getStatus());
    }
}
