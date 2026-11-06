package com.wlstore.inventory_service.model;

import com.wlstore.common.persistence.TenantEntity;
import com.wlstore.inventory_service.enums.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Registro de que um pedido reservou estoque: a identidade do pedido (idempotência) e o estado da reserva.
 * As linhas ficam em {@link ReservationLine}.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Table(name = "tb_reservation")
public class Reservation extends TenantEntity {
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @lombok.Builder.Default
    private ReservationStatus status = ReservationStatus.RESERVED;
}