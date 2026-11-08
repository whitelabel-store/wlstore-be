package com.wlstore.order_service.service;

import com.wlstore.common.tenant.TenantContext;
import com.wlstore.order_service.AbstractIntegrationTest;
import com.wlstore.order_service.dto.OrderLineDTO;
import com.wlstore.order_service.enums.OrderStatus;
import com.wlstore.order_service.gateway.InventoryGateway;
import com.wlstore.order_service.model.Order;
import com.wlstore.order_service.model.OrderLine;
import com.wlstore.order_service.repository.OrderLineRepository;
import com.wlstore.order_service.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;

class CreateOrderTest extends AbstractIntegrationTest {

    @Autowired
    OrderService orderService;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderLineRepository orderLineRepository;

    @Test
    void reservesTheStockAndLeavesTheOrderReservedWhenInventoryAccepts() {
        UUID tenant = UUID.randomUUID();
        List<OrderLineDTO> lines = List.of(new OrderLineDTO("SKU-1", 2), new OrderLineDTO("SKU-2", 1));

        Order created = TenantContext.callWithTenant(tenant, () -> orderService.create(lines));

        verify(inventoryGateway).reserve(created.getTenantId(), created.getId(), lines);

        Order stored = TenantContext.callWithTenant(tenant,
                () -> orderRepository.findById(created.getId()).orElseThrow());
        List<OrderLine> storedLines = TenantContext.callWithTenant(tenant,
                () -> orderLineRepository.findByOrderId(created.getId()));

        assertThat(created.getStatus()).isEqualTo(OrderStatus.RESERVED);
        assertThat(stored.getStatus()).isEqualTo(OrderStatus.RESERVED);
        assertThat(storedLines)
                .extracting(OrderLine::getSku, OrderLine::getQuantity)
                .containsExactlyInAnyOrder(tuple("SKU-1", 2), tuple("SKU-2", 1));
    }
}