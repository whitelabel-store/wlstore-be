package com.wlstore.pricing_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.wlstore.pricing_service.AbstractIntegrationTest;
import com.wlstore.pricing_service.model.Price;
import com.wlstore.pricing_service.repository.PriceRepository;

class PriceServiceTest extends AbstractIntegrationTest {

    @Autowired
    PriceService priceService;

    @Autowired
    PriceRepository priceRepository;

    @Autowired
    SkuSyncService skuSyncService;

    @Test
    void definesFirstPriceAsCurrent() {
        skuSyncService.sync(UUID.randomUUID(), Set.of("SVC-001"));
        priceService.definePrice("SVC-001", 9900L, null);

        Price current = priceRepository.findBySkuAndValidToIsNull("SVC-001").orElseThrow();

        assertThat(current.getAmountCents()).isEqualTo(9900L);
        assertThat(priceRepository.findBySkuOrderByValidFromAsc("SVC-001")).hasSize(1);
    }

    @Test
    void definingNewPriceClosesThePreviousOne() {
        skuSyncService.sync(UUID.randomUUID(), Set.of("SVC-002"));
        priceService.definePrice("SVC-002", 10000L, null);
        priceService.definePrice("SVC-002", 9000L, 10000L);

        List<Price> history = priceRepository.findBySkuOrderByValidFromAsc("SVC-002");

        assertThat(history).hasSize(2);

        Price previous = history.get(0);
        Price current = history.get(1);

        assertThat(previous.getAmountCents()).isEqualTo(10000L);
        assertThat(previous.getValidTo()).isEqualTo(current.getValidFrom());

        assertThat(current.getAmountCents()).isEqualTo(9000L);
        assertThat(current.getOriginalAmountCents()).isEqualTo(10000L);
        assertThat(current.getValidTo()).isNull();
    }

    @Test
    void definingTheSamePriceAgainChangesNothing() {
        skuSyncService.sync(UUID.randomUUID(), Set.of("SVC-003"));
        Price first = priceService.definePrice("SVC-003", 9000L, 10000L);

        Price second = priceService.definePrice("SVC-003", 9000L, 10000L);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(priceRepository.findBySkuOrderByValidFromAsc("SVC-003")).hasSize(1);
        assertThat(priceRepository.findBySkuAndValidToIsNull("SVC-003")).isPresent();
    }

    @Test
    void changingOnlyTheOriginalPriceIsStillANewPeriod() {
        skuSyncService.sync(UUID.randomUUID(), Set.of("SVC-004"));
        priceService.definePrice("SVC-004", 9000L, 10000L);

        priceService.definePrice("SVC-004", 9000L, 12000L);

        List<Price> history = priceRepository.findBySkuOrderByValidFromAsc("SVC-004");

        assertThat(history).hasSize(2);
        assertThat(history.get(1).getOriginalAmountCents()).isEqualTo(12000L);
        assertThat(history.get(1).getValidTo()).isNull();
    }
}