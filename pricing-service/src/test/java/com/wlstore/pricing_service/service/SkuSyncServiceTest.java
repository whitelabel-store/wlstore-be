package com.wlstore.pricing_service.service;

import com.wlstore.pricing_service.AbstractIntegrationTest;
import com.wlstore.pricing_service.model.KnownSku;
import com.wlstore.pricing_service.model.Price;
import com.wlstore.pricing_service.repository.KnownSkuRepository;
import com.wlstore.pricing_service.repository.PriceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SkuSyncServiceTest extends AbstractIntegrationTest {

    @Autowired
    SkuSyncService skuSyncService;

    @Autowired
    PriceService priceService;

    @Autowired
    KnownSkuRepository knownSkuRepository;

    @Autowired
    PriceRepository priceRepository;

    @Test
    void registersTheSkusOfTheProduct() {
        UUID productId = UUID.randomUUID();

        skuSyncService.sync(productId, Set.of("SYNC-A1", "SYNC-A2"));

        List<KnownSku> known = knownSkuRepository.findByProductId(productId);
        assertThat(known).extracting(KnownSku::getSku)
                .containsExactlyInAnyOrder("SYNC-A1", "SYNC-A2");
    }

    @Test
    void closesTheCurrentPriceOfASkuRemovedFromTheProduct() {
        UUID productId = UUID.randomUUID();
        skuSyncService.sync(productId, Set.of("SYNC-B1", "SYNC-B2"));
        priceService.definePrice("SYNC-B2", 7000L, null);

        skuSyncService.sync(productId, Set.of("SYNC-B1"));

        assertThat(knownSkuRepository.findByProductId(productId))
                .extracting(KnownSku::getSku)
                .containsExactly("SYNC-B1");
        assertThat(priceRepository.findBySkuAndValidToIsNull("SYNC-B2")).isEmpty();

        List<Price> history = priceRepository.findBySkuOrderByValidFromAsc("SYNC-B2");
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getValidTo()).isNotNull();
    }
}