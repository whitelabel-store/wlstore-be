package com.wlstore.order_service.service;

import com.wlstore.common.tenant.TenantContext;
import com.wlstore.order_service.AbstractIntegrationTest;
import com.wlstore.order_service.dto.OrderLineDTO;
import com.wlstore.order_service.enums.OrderStatus;
import com.wlstore.order_service.exception.InventoryUnavailableException;
import com.wlstore.order_service.model.Order;
import com.wlstore.order_service.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

class UnknownOutcomeOrderTest extends AbstractIntegrationTest {

    @Autowired
    OrderService orderService;

    @Autowired
    OrderRepository orderRepository;

    @Test
    void leavesTheOrderPendingWhenTheOutcomeOfTheReservationIsUnknown() {
        UUID tenant = UUID.randomUUID();
        List<OrderLineDTO> lines = List.of(new OrderLineDTO("SKU-1", 1));
        doThrow(new InventoryUnavailableException("Inventory did not answer"))
                .when(inventoryGateway).reserve(any(), any(), eq(lines));

        assertThatThrownBy(() -> TenantContext.callWithTenant(tenant, () -> orderService.create(lines)))
                .isInstanceOf(InventoryUnavailableException.class);

        List<Order> orders = TenantContext.callWithTenant(tenant, () -> orderRepository.findAll());

        assertThat(orders).hasSize(1);
        assertThat(orders.getFirst().getStatus()).isEqualTo(OrderStatus.PENDING);
    }
}