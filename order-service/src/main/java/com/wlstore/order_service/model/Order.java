package com.wlstore.order_service.model;

import com.wlstore.common.persistence.TenantEntity;
import com.wlstore.order_service.enums.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Um pedido do cliente. Nasce sempre PENDING: o estoque ainda não foi reservado.
 * A tabela se chama tb_order porque ORDER é palavra reservada do SQL.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Table(name = "tb_order")
public class Order extends TenantEntity {
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;
}
