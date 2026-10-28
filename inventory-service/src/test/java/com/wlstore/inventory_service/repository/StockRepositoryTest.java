package com.wlstore.inventory_service.repository;

import com.wlstore.common.tenant.TenantContext;
import com.wlstore.inventory_service.AbstractIntegrationTest;
import com.wlstore.inventory_service.model.Stock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    StockRepository stockRepository;

    @Test
    void storesStockBySkuAndKeepsItInsideTheTenant() {
        UUID tenant = UUID.randomUUID();
        UUID otherTenant = UUID.randomUUID();

        save(tenant, "SKU-1", 10, 3);

        Optional<Stock> found = TenantContext.callWithTenant(tenant, () -> stockRepository.findBySku("SKU-1"));
        Optional<Stock> fromOtherTenant = TenantContext.callWithTenant(otherTenant, () -> stockRepository.findBySku("SKU-1"));

        assertThat(found).isPresent();
        assertThat(found.get().getQuantityOnHand()).isEqualTo(10);
        assertThat(found.get().getQuantityReserved()).isEqualTo(3);
        assertThat(found.get().available()).isEqualTo(7);
        assertThat(fromOtherTenant).isEmpty();
    }

    @Test
    void rejectsNegativeQuantityOnHand() {
        assertThatThrownBy(() -> save(UUID.randomUUID(), "NEG-ON-HAND", -1, 0))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_stock_on_hand_non_negative");
    }

    @Test
    void rejectsNegativeQuantityReserved() {
        assertThatThrownBy(() -> save(UUID.randomUUID(), "NEG-RESERVED", 5, -1))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_stock_reserved_non_negative");
    }

    @Test
    void rejectsReservingMoreThanWhatIsOnHand() {
        assertThatThrownBy(() -> save(UUID.randomUUID(), "OVER-RESERVED", 5, 6))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_stock_reserved_within_on_hand");
    }

    @Test
    void rejectsTwoStockRowsForTheSameSkuInTheSameTenant() {
        UUID tenant = UUID.randomUUID();
        save(tenant, "DUP-SKU", 10, 0);

        assertThatThrownBy(() -> save(tenant, "DUP-SKU", 20, 0))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_stock_tenant_sku");
    }

    @Test
    void allowsTheSameSkuInAnotherTenant() {
        save(UUID.randomUUID(), "SHARED-SKU", 10, 0);

        assertThat(save(UUID.randomUUID(), "SHARED-SKU", 20, 0).getId()).isNotNull();
    }

    private Stock save(UUID tenant, String sku, int onHand, int reserved) {
        return TenantContext.callWithTenant(tenant, () -> stockRepository.saveAndFlush(
                Stock.builder()
                        .sku(sku)
                        .quantityOnHand(onHand)
                        .quantityReserved(reserved)
                        .build()));
    }
}