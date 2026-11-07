package com.wlstore.order_service.repository;

import com.wlstore.common.tenant.TenantContext;
import com.wlstore.order_service.AbstractIntegrationTest;
import com.wlstore.order_service.enums.OrderStatus;
import com.wlstore.order_service.model.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    OrderRepository orderRepository;

    @Test
    void storesAPendingOrderAndKeepsItInsideTheTenant() {
        UUID tenant = UUID.randomUUID();
        UUID otherTenant = UUID.randomUUID();

        Order saved = TenantContext.callWithTenant(tenant,
                () -> orderRepository.save(Order.builder().status(OrderStatus.PENDING).build()));

        List<Order> fromTenant = TenantContext.callWithTenant(tenant, () -> orderRepository.findAll());
        List<Order> fromOtherTenant = TenantContext.callWithTenant(otherTenant, () -> orderRepository.findAll());

        assertThat(fromTenant).extracting(Order::getId).containsExactly(saved.getId());
        assertThat(fromTenant.getFirst().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(fromTenant.getFirst().getTenantId()).isEqualTo(tenant);
        assertThat(fromOtherTenant).isEmpty();
    }
}