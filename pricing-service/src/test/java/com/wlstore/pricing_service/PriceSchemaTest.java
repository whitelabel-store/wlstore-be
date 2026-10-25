package com.wlstore.pricing_service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class PriceSchemaTest extends AbstractIntegrationTest {

    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void rejectsOriginalPriceNotGreaterThanCurrentPrice() {
        assertThatThrownBy(() -> insertPrice("CAM-P", 9900L, 9900L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void acceptsPriceWithoutOriginalPrice() {
        assertThatCode(() -> insertPrice("CAM-M", 9900L, null))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsTwoCurrentPricesForSameSku() {
        insertPrice("CAM-G", 9900L, null);

        assertThatThrownBy(() -> insertPrice("CAM-G", 8900L, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertPrice(String sku, long amountCents, Long originalAmountCents) {
        jdbc.update("""
                insert into tb_price
                    (id, created_at, updated_at, version, tenant_id, sku,
                     amount_cents, original_amount_cents, valid_from)
                values (?, now(), now(), 0, ?, ?, ?, ?, now())
                """,
                UUID.randomUUID(), TENANT, sku, amountCents, originalAmountCents);
    }
}