package com.wlstore.order_service.gateway;

import com.wlstore.order_service.dto.OrderLineDTO;

import java.util.List;
import java.util.UUID;

/**
 * A porta do order-service para o inventory. A regra do pedido só conhece esta interface:
 * como a chamada acontece (HTTP hoje, mensageria amanhã) é detalhe de quem a implementa.
 */
public interface InventoryGateway {
    /**
     * Reserva todas as linhas de uma vez, idempotente por orderId.
     */
    void reserve(UUID tenantId, UUID orderId, List<OrderLineDTO> lines);
}
