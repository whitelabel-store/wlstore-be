package com.wlstore.order_service.service;

import com.wlstore.order_service.dto.OrderLineDTO;
import com.wlstore.order_service.model.Order;

import java.util.List;

public interface OrderService {
    Order create(List<OrderLineDTO> lines);
}
