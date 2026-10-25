package com.wlstore.catalog_service.model;


import com.wlstore.catalog_service.enums.ActiveStatus;
import com.wlstore.common.persistence.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

@Table(name = "tb_product_variant", uniqueConstraints =
@UniqueConstraint(name = "uk_variant_tenant_sku", columnNames = {"tenant_id", "sku"}))
@Entity
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ProductVariant extends TenantEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "sku", nullable = false, updatable = false, length = 64)
    private String sku;

    @Column(name = "name", nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "option_values", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> optionValues = new HashMap<>();

    @Column(name = "weight_grams")
    private Integer weightGrams;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ActiveStatus status = ActiveStatus.ACTIVE;

    @Column(name = "position", nullable = false)
    @Builder.Default
    private int position = 0;
}
