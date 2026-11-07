package com.wlstore.order_service.service;

import com.wlstore.common.tenant.TenantContext;
import com.wlstore.order_service.AbstractIntegrationTest;
import com.wlstore.order_service.dto.OrderLineDTO;
import com.wlstore.order_service.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

class CreateOrderValidationTest extends AbstractIntegrationTest {

    @Autowired
    OrderService orderService;

    @Autowired
    OrderRepository orderRepository;

    @Test
    void rejectsAnOrderWithoutLinesAndCreatesNothing() {
        UUID tenant = UUID.randomUUID();

        assertThatThrownBy(() -> TenantContext.callWithTenant(tenant, () -> orderService.create(List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("line");

        assertNothingWasCreatedOrReserved(tenant);
    }

    @Test
    void rejectsALineWithAQuantityThatIsNotPositiveAndCreatesNothing() {
        UUID tenant = UUID.randomUUID();
        List<OrderLineDTO> lines = List.of(new OrderLineDTO("SKU-1", 1), new OrderLineDTO("SKU-2", 0));

        assertThatThrownBy(() -> TenantContext.callWithTenant(tenant, () -> orderService.create(lines)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU-2");

        assertNothingWasCreatedOrReserved(tenant);
    }

    private void assertNothingWasCreatedOrReserved(UUID tenant) {
        assertThat(TenantContext.callWithTenant(tenant, () -> orderRepository.findAll())).isEmpty();
        verifyNoInteractions(inventoryGateway);
    }
}