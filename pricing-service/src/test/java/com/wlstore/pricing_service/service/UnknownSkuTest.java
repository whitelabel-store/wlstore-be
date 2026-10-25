package com.wlstore.pricing_service.service;

import com.wlstore.pricing_service.AbstractIntegrationTest;
import com.wlstore.pricing_service.exception.UnknownSkuException;
import com.wlstore.pricing_service.repository.PriceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnknownSkuTest extends AbstractIntegrationTest {

    @Autowired
    PriceService priceService;

    @Autowired
    PriceRepository priceRepository;

    @Test
    void rejectsPriceForASkuTheCatalogNeverAnnounced() {
        assertThatThrownBy(() -> priceService.definePrice("UNKNOWN-001", 1000L, null))
                .isInstanceOf(UnknownSkuException.class);

        assertThat(priceRepository.findBySkuAndValidToIsNull("UNKNOWN-001")).isEmpty();
    }
}