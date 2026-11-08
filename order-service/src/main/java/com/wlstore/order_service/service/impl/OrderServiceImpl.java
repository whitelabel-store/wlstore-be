package com.wlstore.order_service.service.impl;

import com.wlstore.order_service.dto.OrderLineDTO;
import com.wlstore.order_service.enums.OrderStatus;
import com.wlstore.order_service.exception.ReservationRejectedException;
import com.wlstore.order_service.gateway.InventoryGateway;
import com.wlstore.order_service.model.Order;
import com.wlstore.order_service.model.OrderLine;
import com.wlstore.order_service.repository.OrderLineRepository;
import com.wlstore.order_service.repository.OrderRepository;
import com.wlstore.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final InventoryGateway inventoryGateway;
    private final TransactionTemplate transactionTemplate;

    /**
     * 0. valida a entrada: inválida não cria nada nem chama ninguém;
     * 1. grava o pedido PENDING com as linhas e faz commit;
     * 2. fora de qualquer transação, pede a reserva ao inventory;
     * 3. grava o resultado: RESERVED, ou REJECTED se o inventory recusou.
     * Se o processo cair depois do passo 1, sobra um pedido PENDING registrado, que dá para recuperar.
     */
    @Override
    public Order create(List<OrderLineDTO> lines) {
        validate(lines);

        Order pending = savePending(lines);

        try {
            inventoryGateway.reserve(pending.getTenantId(), pending.getId(), lines);
        } catch (ReservationRejectedException rejection) {
            markAs(pending.getId(), OrderStatus.REJECTED);
            throw rejection;
        }

        return markAs(pending.getId(), OrderStatus.RESERVED);
    }

    private void validate(List<OrderLineDTO> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("An order needs at least one line");
        }

        for (OrderLineDTO line : lines) {
            if (line.quantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be positive for SKU " + line.sku());
            }
        }
    }

    private Order savePending(List<OrderLineDTO> lines) {
        return transactionTemplate.execute(status -> {
            Order order = orderRepository.save(Order.builder().build());
            for (OrderLineDTO line : lines) {
                orderLineRepository.save(OrderLine.builder()
                        .orderId(order.getId())
                        .sku(line.sku())
                        .quantity(line.quantity())
                        .build());
            }
            return order;
        });
    }

    private Order markAs(UUID orderId, OrderStatus newStatus) {
        return transactionTemplate.execute(status -> {
            Order order = orderRepository.findById(orderId).orElseThrow();
            order.setStatus(newStatus);
            return order;
        });
    }
}