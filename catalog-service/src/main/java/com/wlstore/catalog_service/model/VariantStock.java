package com.wlstore.catalog_service.model;

import com.wlstore.common.persistence.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Cópia de leitura da disponibilidade de um SKU. A fonte da verdade é o inventory-service:
 * esta linha só é escrita pelo consumidor do evento stock.changed.
 * <p>
 * {@code stockVersion} é a version do estoque no inventory (não confundir com o {@code version}
 * do bloqueio otimista, herdado da BaseEntity).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Table(name = "tb_variant_stock")
public class VariantStock extends TenantEntity {
    @Column(name = "sku", nullable = false, updatable = false, length = 64)
    private String sku;

    @Column(name = "available", nullable = false)
    private int available;

    @Column(name = "stock_version", nullable = false)
    private long stockVersion;

}
