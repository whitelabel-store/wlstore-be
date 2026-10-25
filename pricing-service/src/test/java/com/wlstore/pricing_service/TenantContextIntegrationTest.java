package com.wlstore.pricing_service;

import com.wlstore.common.tenant.TenantContext;
import com.wlstore.pricing_service.model.Price;
import com.wlstore.pricing_service.repository.PriceRepository;
import com.wlstore.pricing_service.service.PriceService;
import com.wlstore.pricing_service.service.SkuSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    PriceService priceService;

    @Autowired
    SkuSyncService skuSyncService;

    @Autowired
    PriceRepository priceRepository;

    @Test
    void storesPriceUnderTheExplicitTenant() {
        UUID tenant = UUID.randomUUID();

        TenantContext.runWithTenant(tenant, () -> {
            skuSyncService.sync(UUID.randomUUID(), Set.of("TEN-001"));
            priceService.definePrice("TEN-001", 5000L, null);
        });

        Price saved = TenantContext.callWithTenant(tenant,
                () -> priceRepository.findBySkuAndValidToIsNull("TEN-001").orElseThrow());
        assertThat(saved.getTenantId()).isEqualTo(tenant);
    }

    @Test
    void otherTenantsCannotSeeThePrice() {
        UUID tenant = UUID.randomUUID();

        TenantContext.runWithTenant(tenant, () -> {
            skuSyncService.sync(UUID.randomUUID(), Set.of("TEN-002"));
            priceService.definePrice("TEN-002", 5000L, null);
        });

        assertThat(priceRepository.findBySkuAndValidToIsNull("TEN-002")).isEmpty();
    }
}