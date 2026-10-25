package com.wlstore.catalog_service.repository;

import com.wlstore.catalog_service.AbstractIntegrationTest;
import com.wlstore.catalog_service.model.VariantPrice;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class VariantPriceRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    VariantPriceRepository repository;

    @Autowired
    EntityManager entityManager;

    @Test
    void savesAndReadsBackAPriceBySku() {
        UUID productId = UUID.randomUUID();
        Instant validFrom = Instant.now().truncatedTo(ChronoUnit.MICROS);

        repository.saveAndFlush(price(productId, "VPR-001", 9000L, 10000L, validFrom));
        entityManager.clear();

        VariantPrice found = repository.findBySku("VPR-001").orElseThrow();

        assertThat(found.getProductId()).isEqualTo(productId);
        assertThat(found.getAmountCents()).isEqualTo(9000L);
        assertThat(found.getOriginalAmountCents()).isEqualTo(10000L);
        assertThat(found.getValidFrom()).isEqualTo(validFrom);
    }

    @Test
    void findsOnlyThePricesOfTheRequestedProduct() {
        UUID product = UUID.randomUUID();
        UUID otherProduct = UUID.randomUUID();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

        repository.save(price(product, "VPR-010", 5000L, null, now));
        repository.save(price(product, "VPR-011", 7000L, null, now));
        repository.save(price(otherProduct, "VPR-020", 9000L, null, now));
        repository.flush();
        entityManager.clear();

        List<VariantPrice> prices = repository.findByProductId(product);

        assertThat(prices)
                .extracting(VariantPrice::getSku)
                .containsExactlyInAnyOrder("VPR-010", "VPR-011");
    }

    private VariantPrice price(UUID productId, String sku, long amountCents, Long originalAmountCents, Instant validFrom) {
        return VariantPrice.builder()
                .productId(productId)
                .sku(sku)
                .amountCents(amountCents)
                .originalAmountCents(originalAmountCents)
                .validFrom(validFrom)
                .build();
    }
}