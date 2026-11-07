package com.wlstore.inventory_service.service;

import com.wlstore.common.tenant.TenantContext;
import com.wlstore.inventory_service.AbstractIntegrationTest;
import com.wlstore.inventory_service.dto.ReservationLineDTO;
import com.wlstore.inventory_service.enums.ReservationStatus;
import com.wlstore.inventory_service.exception.ReservationStateException;
import com.wlstore.inventory_service.model.Stock;
import com.wlstore.inventory_service.repository.StockRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockConfirmConflictTest extends AbstractIntegrationTest {

    @Autowired
    StockService stockService;

    @Autowired
    StockRepository stockRepository;

    @Test
    void confirmingAReleasedOrderIsAConflictAndConsumesNothing() {
        UUID tenant = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        givenStock(tenant, "A-1", 10);

        TenantContext.runWithTenant(tenant, () ->
                stockService.reserveAll(orderId, List.of(new ReservationLineDTO("A-1", 3))));
        TenantContext.runWithTenant(tenant, () -> stockService.release(orderId));

        assertThatThrownBy(() -> TenantContext.runWithTenant(tenant, () -> stockService.confirm(orderId)))
                .isInstanceOfSatisfying(ReservationStateException.class, e -> {
                    assertThat(e.getOrderId()).isEqualTo(orderId);
                    assertThat(e.getStatus()).isEqualTo(ReservationStatus.RELEASED);
                });

        // the units went back at the release and the failed confirmation must not touch them
        Stock stock = TenantContext.callWithTenant(tenant, () -> stockRepository.findBySku("A-1").orElseThrow());
        assertThat(stock.getQuantityOnHand()).isEqualTo(10);
        assertThat(stock.getQuantityReserved()).isZero();
    }

    private void givenStock(UUID tenant, String sku, int onHand) {
        TenantContext.runWithTenant(tenant, () -> stockRepository.saveAndFlush(
                Stock.builder().sku(sku).quantityOnHand(onHand).quantityReserved(0).build()));
    }
}