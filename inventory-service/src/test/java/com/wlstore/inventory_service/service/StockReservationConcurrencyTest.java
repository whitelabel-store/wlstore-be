package com.wlstore.inventory_service.service;

import com.wlstore.common.tenant.TenantContext;
import com.wlstore.inventory_service.AbstractIntegrationTest;
import com.wlstore.inventory_service.exception.InsufficientStockException;
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

class StockReservationConcurrencyTest extends AbstractIntegrationTest {

    private static final String SKU = "HOT-SKU";
    private static final int THREADS = 20;
    private static final int ON_HAND = 5;

    @Autowired
    StockService stockService;

    @Autowired
    StockRepository stockRepository;

    @RepeatedTest(5)
    void competingReservationsSellExactlyWhatExistsAndNeverMore() throws Exception {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, ON_HAND);

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
                    return tryToReserveOne(tenant);
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

        Stock stock = TenantContext.callWithTenant(tenant, () -> stockRepository.findBySku(SKU).orElseThrow());

        // safety first: never sell what does not exist
        assertThat(stock.getQuantityReserved()).as("reserved never exceeds on hand").isLessThanOrEqualTo(ON_HAND);

        // liveness: every unit available must be sold, and the losers must be told "insufficient", nothing else
        assertThat(outcomes)
                .as("outcome of each of the %d threads", THREADS)
                .containsOnlyKeys("RESERVED", "INSUFFICIENT")
                .containsEntry("RESERVED", (long) ON_HAND)
                .containsEntry("INSUFFICIENT", (long) (THREADS - ON_HAND));
        assertThat(stock.getQuantityReserved()).isEqualTo(ON_HAND);
    }

    /** Returns RESERVED, INSUFFICIENT, or the simple name of whatever unexpected exception was thrown. */
    private String tryToReserveOne(UUID tenant) {
        try {
            TenantContext.runWithTenant(tenant, () -> stockService.reserve(SKU, 1));
            return "RESERVED";
        } catch (InsufficientStockException e) {
            return "INSUFFICIENT";
        } catch (RuntimeException e) {
            return e.getClass().getSimpleName();
        }
    }

    private void givenStock(UUID tenant, int onHand) {
        TenantContext.runWithTenant(tenant, () -> stockRepository.saveAndFlush(
                Stock.builder()
                        .sku(SKU)
                        .quantityOnHand(onHand)
                        .quantityReserved(0)
                        .build()));
    }
}