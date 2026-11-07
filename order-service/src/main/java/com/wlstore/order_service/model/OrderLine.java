package com.wlstore.order_service.model;

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
 * Uma linha do pedido: quanto de cada SKU o cliente pediu.
 * É um registro histórico: depois de gravada, não muda (as colunas são updatable = false),
 * porque o estoque foi reservado exatamente para essas quantidades.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Table(name = "tb_order_line")
public class OrderLine extends TenantEntity {
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "sku", nullable = false, updatable = false, length = 64)
    private String  sku;

    @Column(name = "quantity", nullable = false, updatable = false)
    private int quantity;
}
