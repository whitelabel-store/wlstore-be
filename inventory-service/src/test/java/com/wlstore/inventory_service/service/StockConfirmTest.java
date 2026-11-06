package com.wlstore.inventory_service.service;

import com.wlstore.common.exception.ResourceNotFoundException;
import com.wlstore.common.tenant.TenantContext;
import com.wlstore.inventory_service.AbstractIntegrationTest;
import com.wlstore.inventory_service.dto.ReservationLineDTO;
import com.wlstore.inventory_service.model.Stock;
import com.wlstore.inventory_service.repository.StockRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockConfirmTest extends AbstractIntegrationTest {

    @Autowired
    StockService stockService;

    @Autowired
    StockRepository stockRepository;

    @Test
    void confirmingAnOrderConsumesTheReservedUnitsOfEveryLine() {
        UUID tenant = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        givenStock(tenant, "A-1", 10);
        givenStock(tenant, "B-1", 5);

        reserve(tenant, orderId, line("A-1", 3), line("B-1", 2));

        confirm(tenant, orderId);

        // the goods left: on hand dropped, nothing is held any more, and what was left for sale is unchanged
        Stock a = stockOf(tenant, "A-1");
        assertThat(a.getQuantityOnHand()).isEqualTo(7);
        assertThat(a.getQuantityReserved()).isZero();
        assertThat(a.available()).isEqualTo(7);

        Stock b = stockOf(tenant, "B-1");
        assertThat(b.getQuantityOnHand()).isEqualTo(3);
        assertThat(b.getQuantityReserved()).isZero();
        assertThat(b.available()).isEqualTo(3);
    }

    @Test
    void confirmingTheSameOrderTwiceConsumesOnlyOnce() {
        UUID tenant = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        givenStock(tenant, "A-1", 20);

        reserve(tenant, first, line("A-1", 3));
        reserve(tenant, second, line("A-1", 4));

        confirm(tenant, first);
        confirm(tenant, first);   // the retry after a lost response

        // 3 units left the warehouse once; the 4 of the second order are still held
        Stock stock = stockOf(tenant, "A-1");
        assertThat(stock.getQuantityOnHand()).isEqualTo(17);
        assertThat(stock.getQuantityReserved()).isEqualTo(4);
    }

    @Test
    void confirmingAnOrderThatNeverReservedIsNotFoundAndChangesNothing() {
        UUID tenant = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        givenStock(tenant, "A-1", 10);
        reserve(tenant, UUID.randomUUID(), line("A-1", 2));

        assertThatThrownBy(() -> confirm(tenant, unknown))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(unknown.toString());

        Stock stock = stockOf(tenant, "A-1");
        assertThat(stock.getQuantityOnHand()).isEqualTo(10);
        assertThat(stock.getQuantityReserved()).isEqualTo(2);
    }

    @Test
    void releasingAConfirmedOrderGivesNothingBack() {
        UUID tenant = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        givenStock(tenant, "A-1", 10);

        reserve(tenant, orderId, line("A-1", 3));
        confirm(tenant, orderId);

        TenantContext.runWithTenant(tenant, () -> stockService.release(orderId));

        // the goods already left: the late release must not resurrect them
        Stock stock = stockOf(tenant, "A-1");
        assertThat(stock.getQuantityOnHand()).isEqualTo(7);
        assertThat(stock.getQuantityReserved()).isZero();
    }

    // ---------- helpers ----------

    private ReservationLineDTO line(String sku, int quantity) {
        return new ReservationLineDTO(sku, quantity);
    }

    private void reserve(UUID tenant, UUID orderId, ReservationLineDTO... lines) {
        TenantContext.runWithTenant(tenant, () -> stockService.reserveAll(orderId, List.of(lines)));
    }

    private void confirm(UUID tenant, UUID orderId) {
        TenantContext.runWithTenant(tenant, () -> stockService.confirm(orderId));
    }

    private void givenStock(UUID tenant, String sku, int onHand) {
        TenantContext.runWithTenant(tenant, () -> stockRepository.saveAndFlush(
                Stock.builder().sku(sku).quantityOnHand(onHand).quantityReserved(0).build()));
    }

    private Stock stockOf(UUID tenant, String sku) {
        return TenantContext.callWithTenant(tenant, () -> stockRepository.findBySku(sku).orElseThrow());
    }
}