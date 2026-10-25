package com.wlstore.catalog_service.model;

import com.wlstore.common.persistence.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

/**
 * Cópia de leitura do preço vigente de um SKU. A fonte da verdade é o pricing-service:
 * esta linha só é escrita pelo consumidor do evento price.changed.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Table(name = "tb_variant_price")
public class VariantPrice extends TenantEntity {
    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "sku", nullable = false, updatable = false, length = 64)
    private String sku;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(name = "original_amount_cents")
    private Long originalAmountCents;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;
}
