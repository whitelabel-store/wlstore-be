package com.wlstore.inventory_service.service;

import com.wlstore.common.tenant.TenantContext;
import com.wlstore.inventory_service.AbstractIntegrationTest;
import com.wlstore.inventory_service.dto.ReservationLineDTO;
import com.wlstore.inventory_service.exception.InsufficientStockException;
import com.wlstore.inventory_service.model.Stock;
import com.wlstore.inventory_service.repository.StockRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockReleaseTest extends AbstractIntegrationTest {

    @Autowired
    StockService stockService;

    @Autowired
    StockRepository stockRepository;

    @Test
    void releasingAnOrderGivesEveryLineBack() {
        UUID tenant = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        givenStock(tenant, "A-1", 10);
        givenStock(tenant, "B-1", 5);

        reserve(tenant, orderId, line("A-1", 3), line("B-1", 2));
        assertThat(reservedOf(tenant, "A-1")).isEqualTo(3);
        assertThat(reservedOf(tenant, "B-1")).isEqualTo(2);

        release(tenant, orderId);

        assertThat(reservedOf(tenant, "A-1")).isZero();
        assertThat(reservedOf(tenant, "B-1")).isZero();
    }

    @Test
    void unitsFreedByAReleaseCanBeReservedByAnotherOrder() {
        UUID tenant = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        givenStock(tenant, "A-1", 5);

        reserve(tenant, first, line("A-1", 5));
        assertThatThrownBy(() -> reserve(tenant, second, line("A-1", 5)))
                .isInstanceOf(InsufficientStockException.class);

        release(tenant, first);
        reserve(tenant, second, line("A-1", 5));

        assertThat(reservedOf(tenant, "A-1")).isEqualTo(5);
    }

    @Test
    void releasingTheSameOrderTwiceGivesTheUnitsBackOnlyOnce() {
        UUID tenant = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        givenStock(tenant, "A-1", 20);

        reserve(tenant, first, line("A-1", 3));
        reserve(tenant, second, line("A-1", 4));
        assertThat(reservedOf(tenant, "A-1")).isEqualTo(7);

        release(tenant, first);
        release(tenant, first);   // the retry after a lost response

        // only the 3 units of the first order went back; the 4 of the second are still held
        assertThat(reservedOf(tenant, "A-1")).isEqualTo(4);
    }

    @Test
    void releasingAnUnknownOrderChangesNothing() {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "A-1", 10);
        reserve(tenant, UUID.randomUUID(), line("A-1", 4));

        release(tenant, UUID.randomUUID());   // never reserved anything

        assertThat(reservedOf(tenant, "A-1")).isEqualTo(4);
    }

    // ---------- helpers ----------

    private ReservationLineDTO line(String sku, int quantity) {
        return new ReservationLineDTO(sku, quantity);
    }

    private void reserve(UUID tenant, UUID orderId, ReservationLineDTO... lines) {
        TenantContext.runWithTenant(tenant, () -> stockService.reserveAll(orderId, List.of(lines)));
    }

    private void release(UUID tenant, UUID orderId) {
        TenantContext.runWithTenant(tenant, () -> stockService.release(orderId));
    }

    private void givenStock(UUID tenant, String sku, int onHand) {
        TenantContext.runWithTenant(tenant, () -> stockRepository.saveAndFlush(
                Stock.builder().sku(sku).quantityOnHand(onHand).quantityReserved(0).build()));
    }

    private int reservedOf(UUID tenant, String sku) {
        return TenantContext.callWithTenant(tenant,
                () -> stockRepository.findBySku(sku).orElseThrow().getQuantityReserved());
    }
}