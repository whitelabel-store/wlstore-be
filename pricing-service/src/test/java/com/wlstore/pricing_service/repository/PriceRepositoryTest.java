package com.wlstore.pricing_service.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import com.wlstore.pricing_service.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.wlstore.common.tenant.RequestTenantResolver;
import com.wlstore.pricing_service.model.Price;

class PriceRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    PriceRepository priceRepository;

    @Test
    void savesPriceAndFindsCurrentBySku() {
        Price price = Price.builder()
                .sku("REPO-001")
                .amountCents(9900L)
                .originalAmountCents(12000L)
                .validFrom(Instant.now())
                .build();

        priceRepository.saveAndFlush(price);

        Price found = priceRepository.findBySkuAndValidToIsNull("REPO-001").orElseThrow();

        assertThat(found.getAmountCents()).isEqualTo(9900L);
        assertThat(found.getOriginalAmountCents()).isEqualTo(12000L);
        assertThat(found.getValidTo()).isNull();
        assertThat(found.getTenantId()).isEqualTo(RequestTenantResolver.DEFAULT_TENANT);
    }
}