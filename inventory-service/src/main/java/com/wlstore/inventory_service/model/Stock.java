package com.wlstore.inventory_service.model;

import com.wlstore.common.persistence.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Saldo de um SKU: quanto existe fisicamente e quanto já está reservado por pedidos.
 * O disponível não é coluna: é derivado, para nunca divergir dos dois números que o compõem.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Table(name = "tb_stock")
public class Stock extends TenantEntity {
    @Column(name = "sku", nullable = false, updatable = false, length = 64)
    private String sku;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;

    @Column(name = "quantity_reserved", nullable = false)
    private int quantityReserved;

    public int available() {
        return quantityOnHand - quantityReserved;
    }
}
