package com.wlstore.inventory_service.service;

import com.wlstore.common.tenant.TenantContext;
import com.wlstore.inventory_service.AbstractIntegrationTest;
import com.wlstore.inventory_service.dto.ReservationLineDTO;
import com.wlstore.inventory_service.model.Stock;
import com.wlstore.inventory_service.repository.StockRepository;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class StockBulkReservationLineConcurrencyTest extends AbstractIntegrationTest {

    private static final String SKU = "HOT-SKU";
    private static final int THREADS = 10;
    private static final int ON_HAND = 100;
    private static final int QUANTITY = 2;

    @Autowired
    StockService stockService;

    @Autowired
    StockRepository stockRepository;

    @RepeatedTest(5)
    void theSameOrderSentAtTheSameInstantReservesOnceAndEveryCallSucceeds() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        givenStock(tenant);

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> results = new ArrayList<>();

        Map<String, Long> outcomes = new TreeMap<>();
        try {
            for (int i = 0; i < THREADS; i++) {
                results.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();          // every thread waits here until all of them are ready
                    return tryToReserve(tenant, orderId);
                }));
            }

            ready.await();
            start.countDown();              // starting gun: all threads go at the same instant

            for (Future<String> result : results) {
                outcomes.merge(result.get(30, TimeUnit.SECONDS), 1L, Long::sum);
            }
        } finally {
            pool.shutdownNow();
        }

        int reserved = TenantContext.callWithTenant(tenant,
                () -> stockRepository.findBySku(SKU).orElseThrow().getQuantityReserved());

        // safety: the order is counted once, however many times and however fast it was sent
        assertThat(reserved).as("units reserved").isEqualTo(QUANTITY);

        // contract: a repeated order is a success for everybody, not an error for the ones that lost the race
        assertThat(outcomes)
                .as("outcome of each of the %d threads", THREADS)
                .containsOnlyKeys("OK")
                .containsEntry("OK", (long) THREADS);
    }

    /** Returns OK, or the simple name of whatever exception was thrown. */
    private String tryToReserve(UUID tenant, UUID orderId) {
        try {
            TenantContext.runWithTenant(tenant, () ->
                    stockService.reserveAll(orderId, List.of(new ReservationLineDTO(SKU, QUANTITY))));
            return "OK";
        } catch (RuntimeException e) {
            return e.getClass().getSimpleName();
        }
    }

    private void givenStock(UUID tenant) {
        TenantContext.runWithTenant(tenant, () -> stockRepository.saveAndFlush(
                Stock.builder().sku(SKU).quantityOnHand(ON_HAND).quantityReserved(0).build()));
    }
}