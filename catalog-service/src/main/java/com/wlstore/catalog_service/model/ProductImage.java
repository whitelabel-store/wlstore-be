package com.wlstore.catalog_service.model;

import com.wlstore.common.persistence.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Table(name = "tb_product_image")
@Entity
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ProductImage extends TenantEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "alt_text", length = 200)
    private String altText;

    @Column(name = "position", nullable = false)
    @Builder.Default
    private int position = 0;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean primaryImage = false;
}
