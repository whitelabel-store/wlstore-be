package com.wlstore.inventory_service.service;

import com.wlstore.common.exception.ResourceNotFoundException;
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

class StockBulkReservationLineTest extends AbstractIntegrationTest {

    @Autowired
    StockService stockService;

    @Autowired
    StockRepository stockRepository;

    @Test
    void reservesEveryLineWhenAllHaveStock() {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "A-1", 10);
        givenStock(tenant, "B-1", 5);

        TenantContext.runWithTenant(tenant, () ->
                stockService.reserveAll(UUID.randomUUID(), List.of(line("A-1", 3), line("B-1", 2))));

        assertThat(reservedOf(tenant, "A-1")).isEqualTo(3);
        assertThat(reservedOf(tenant, "B-1")).isEqualTo(2);
    }

    @Test
    void reservesNothingWhenALaterLineLacksStock() {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "A-1", 10);
        givenStock(tenant, "B-1", 1);

        // A-1 comes first and has stock; B-1 comes second and does not
        assertThatThrownBy(() -> TenantContext.runWithTenant(tenant, () ->
                stockService.reserveAll(UUID.randomUUID(), List.of(line("A-1", 3), line("B-1", 2)))))
                .isInstanceOfSatisfying(InsufficientStockException.class, e -> {
                    assertThat(e.getSku()).isEqualTo("B-1");
                    assertThat(e.getRequested()).isEqualTo(2);
                    assertThat(e.getAvailable()).isEqualTo(1);
                });

        // the first line was already written when the second failed: it must have been rolled back
        assertThat(reservedOf(tenant, "A-1")).isZero();
        assertThat(reservedOf(tenant, "B-1")).isZero();
    }

    @Test
    void reservesNothingWhenALaterLineHasAnUnknownSku() {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "A-1", 10);

        assertThatThrownBy(() -> TenantContext.runWithTenant(tenant, () ->
                stockService.reserveAll(UUID.randomUUID(), List.of(line("A-1", 3), line("GHOST-1", 1)))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("GHOST-1");

        assertThat(reservedOf(tenant, "A-1")).isZero();
    }

    // ---------- idempotency by orderId ----------

    @Test
    void theSameOrderReservedTwiceCountsOnce() {
        UUID tenant = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        givenStock(tenant, "A-1", 10);

        // the second call simulates the retry after a lost response
        TenantContext.runWithTenant(tenant, () -> stockService.reserveAll(orderId, List.of(line("A-1", 3))));
        TenantContext.runWithTenant(tenant, () -> stockService.reserveAll(orderId, List.of(line("A-1", 3))));

        assertThat(reservedOf(tenant, "A-1")).isEqualTo(3);
    }

    @Test
    void aFailedAttemptDoesNotConsumeTheOrderId() {
        UUID tenant = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        givenStock(tenant, "A-1", 10);

        // first attempt: C-1 does not exist yet, so everything is rolled back
        assertThatThrownBy(() -> TenantContext.runWithTenant(tenant, () ->
                stockService.reserveAll(orderId, List.of(line("A-1", 3), line("C-1", 1)))))
                .isInstanceOf(ResourceNotFoundException.class);

        // C-1 shows up; the same order retries and must go through, reserving A-1 exactly once
        givenStock(tenant, "C-1", 5);
        TenantContext.runWithTenant(tenant, () ->
                stockService.reserveAll(orderId, List.of(line("A-1", 3), line("C-1", 1))));

        assertThat(reservedOf(tenant, "A-1")).isEqualTo(3);
        assertThat(reservedOf(tenant, "C-1")).isEqualTo(1);
    }

    // ---------- helpers ----------

    private ReservationLineDTO line(String sku, int quantity) {
        return new ReservationLineDTO(sku, quantity);
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