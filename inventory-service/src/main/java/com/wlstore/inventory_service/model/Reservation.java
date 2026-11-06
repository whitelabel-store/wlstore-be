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
 * Registro de que um pedido reservou estoque. Hoje só guarda a identidade do pedido (idempotência);
 * as linhas e o estado (liberada, confirmada) entram quando precisarmos delas.
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
}
