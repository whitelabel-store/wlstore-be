package com.wlstore.order_service.repository;

import com.wlstore.order_service.model.OrderLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderLineRepository extends JpaRepository<OrderLine, UUID> {
    List<OrderLine> findByOrderId(UUID orderId);
}
