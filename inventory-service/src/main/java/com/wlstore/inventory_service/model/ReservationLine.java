package com.wlstore.inventory_service.model;

import com.wlstore.common.persistence.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * One line an order reserved: how many units of which SKU. It is what a release or a confirmation walks over.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Table(name = "tb_reservation_line")
public class ReservationLine extends TenantEntity {
    @Column(name = "reservation_id", nullable = false, updatable = false)
    private UUID reservationId;

    @Column(name = "sku", nullable = false, updatable = false, length = 64)
    private String sku;

    @Column(name = "quantity", nullable = false, updatable = false)
    private int quantity;
}