package com.wlstore.inventory_service.service;

import com.wlstore.common.exception.ResourceNotFoundException;
import com.wlstore.common.tenant.TenantContext;
import com.wlstore.inventory_service.AbstractIntegrationTest;
import com.wlstore.inventory_service.model.Stock;
import com.wlstore.inventory_service.repository.StockRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockReservationTenantIsolationTest extends AbstractIntegrationTest {

    private static final String SKU = "SAME-SKU";

    @Autowired
    StockService stockService;

    @Autowired
    StockRepository stockRepository;

    @Test
    void reservingInOneTenantDoesNotTouchTheSameSkuOfAnotherTenant() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        givenStock(tenantA, 10);
        givenStock(tenantB, 10);

        TenantContext.runWithTenant(tenantA, () -> stockService.reserve(SKU, 4));

        assertThat(reservedOf(tenantA)).isEqualTo(4);
        assertThat(reservedOf(tenantB)).isZero();
    }

    @Test
    void aTenantCannotReserveStockThatBelongsToAnotherTenant() {
        UUID owner = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        givenStock(owner, 10);

        assertThatThrownBy(() -> TenantContext.runWithTenant(stranger, () -> stockService.reserve(SKU, 1)))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(reservedOf(owner)).isZero();
    }

    private void givenStock(UUID tenant, int onHand) {
        TenantContext.runWithTenant(tenant, () -> stockRepository.saveAndFlush(
                Stock.builder().sku(SKU).quantityOnHand(onHand).quantityReserved(0).build()));
    }

    private int reservedOf(UUID tenant) {
        return TenantContext.callWithTenant(tenant,
                () -> stockRepository.findBySku(SKU).orElseThrow().getQuantityReserved());
    }
}