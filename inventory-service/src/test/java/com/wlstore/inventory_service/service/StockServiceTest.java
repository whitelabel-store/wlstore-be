package com.wlstore.inventory_service.service;

import com.wlstore.common.exception.ResourceNotFoundException;
import com.wlstore.common.tenant.TenantContext;
import com.wlstore.inventory_service.AbstractIntegrationTest;
import com.wlstore.inventory_service.exception.InsufficientStockException;
import com.wlstore.inventory_service.model.Stock;
import com.wlstore.inventory_service.repository.StockRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockServiceTest extends AbstractIntegrationTest {

    @Autowired
    StockService stockService;

    @Autowired
    StockRepository stockRepository;

    @Test
    void reservingWithinTheAvailableQuantityRaisesReservedAndLowersAvailable() {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "SKU-A", 10, 0);

        reserve(tenant, "SKU-A", 4);

        Stock stock = stockOf(tenant, "SKU-A");
        assertThat(stock.getQuantityReserved()).isEqualTo(4);
        assertThat(stock.available()).isEqualTo(6);
    }

    @Test
    void reservingExactlyTheAvailableQuantityIsAllowed() {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "SKU-B", 5, 0);

        reserve(tenant, "SKU-B", 5);

        assertThat(stockOf(tenant, "SKU-B").available()).isZero();
    }

    @Test
    void reservingMoreThanAvailableFailsAndLeavesTheStockUntouched() {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "SKU-C", 5, 3); // available = 2

        assertThatThrownBy(() -> reserve(tenant, "SKU-C", 3))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(stockOf(tenant, "SKU-C").getQuantityReserved()).isEqualTo(3);
    }

    @Test
    void reservingASkuWithoutStockIsNotFound() {
        assertThatThrownBy(() -> reserve(UUID.randomUUID(), "NO-SUCH-SKU", 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reservingRequiresAPositiveQuantity() {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "SKU-D", 5, 0);

        assertThatThrownBy(() -> reserve(tenant, "SKU-D", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reserve(tenant, "SKU-D", -2)).isInstanceOf(IllegalArgumentException.class);
    }

    private void reserve(UUID tenant, String sku, int quantity) {
        TenantContext.runWithTenant(tenant, () -> stockService.reserve(sku, quantity));
    }

    private void givenStock(UUID tenant, String sku, int onHand, int reserved) {
        TenantContext.runWithTenant(tenant, () -> stockRepository.saveAndFlush(
                Stock.builder()
                        .sku(sku)
                        .quantityOnHand(onHand)
                        .quantityReserved(reserved)
                        .build()));
    }

    private Stock stockOf(UUID tenant, String sku) {
        return TenantContext.callWithTenant(tenant, () -> stockRepository.findBySku(sku).orElseThrow());
    }
}