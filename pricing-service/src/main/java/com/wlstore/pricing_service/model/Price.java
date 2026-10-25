package com.wlstore.pricing_service.model;

import com.wlstore.common.persistence.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Table(name = "tb_price")
public class Price extends TenantEntity {
    @Column(name = "sku", nullable = false, updatable = false, length = 64)
    private String sku;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(name = "original_amount_cents")
    private Long originalAmountCents;

    @Column(name = "valid_from", nullable = false, updatable = false)
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;
}
