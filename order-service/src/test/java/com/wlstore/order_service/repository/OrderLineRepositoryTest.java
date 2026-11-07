package com.wlstore.order_service.repository;

import com.wlstore.common.tenant.TenantContext;
import com.wlstore.order_service.AbstractIntegrationTest;
import com.wlstore.order_service.model.Order;
import com.wlstore.order_service.model.OrderLine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class OrderLineRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderLineRepository orderLineRepository;

    @Test
    void storesTheLinesOfAnOrderInsideTheTenant() {
        UUID tenant = UUID.randomUUID();
        UUID otherTenant = UUID.randomUUID();
        Order order = saveOrder(tenant);

        saveLine(tenant, order.getId(), "SKU-1", 2);
        saveLine(tenant, order.getId(), "SKU-2", 1);

        List<OrderLine> lines = TenantContext.callWithTenant(tenant,
                () -> orderLineRepository.findByOrderId(order.getId()));
        List<OrderLine> fromOtherTenant = TenantContext.callWithTenant(otherTenant,
                () -> orderLineRepository.findByOrderId(order.getId()));

        assertThat(lines)
                .extracting(OrderLine::getSku, OrderLine::getQuantity)
                .containsExactlyInAnyOrder(tuple("SKU-1", 2), tuple("SKU-2", 1));
        assertThat(fromOtherTenant).isEmpty();
    }

    @Test
    void rejectsALineWithAQuantityThatIsNotPositive() {
        UUID tenant = UUID.randomUUID();
        Order order = saveOrder(tenant);

        assertThatThrownBy(() -> saveLine(tenant, order.getId(), "SKU-1", 0))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_order_line_quantity_positive");
    }

    private Order saveOrder(UUID tenant) {
        return TenantContext.callWithTenant(tenant, () -> orderRepository.save(Order.builder().build()));
    }

    private OrderLine saveLine(UUID tenant, UUID orderId, String sku, int quantity) {
        return TenantContext.callWithTenant(tenant, () -> orderLineRepository.save(
                OrderLine.builder().orderId(orderId).sku(sku).quantity(quantity).build()));
    }
}