package com.wlstore.catalog_service.outbox;

import com.wlstore.catalog_service.enums.ActiveStatus;
import com.wlstore.catalog_service.enums.ProductStatus;
import com.wlstore.catalog_service.model.Product;
import com.wlstore.catalog_service.model.ProductVariant;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSnapshotTest {

    @Test
    void fromProductCopiesOnlyWhatConsumersNeed() {
        UUID productId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        Product product = Product.builder()
                .id(productId)
                .tenantId(tenantId)
                .title("Camiseta")
                .brand("Marca")
                .description("Descrição")
                .status(ProductStatus.ACTIVE)
                .build();
        product.addVariant(ProductVariant.builder().sku("CAM-P").name("P").status(ActiveStatus.ACTIVE).build());
        product.addVariant(ProductVariant.builder().sku("CAM-M").name("M").status(ActiveStatus.INACTIVE).build());

        ProductSnapshot snapshot = ProductSnapshot.from(product);

        assertThat(snapshot.productId()).isEqualTo(productId);
        assertThat(snapshot.tenantId()).isEqualTo(tenantId);
        assertThat(snapshot.status()).isEqualTo("ACTIVE");
        assertThat(snapshot.variants())
                .extracting(ProductSnapshot.VariantSnapshot::sku, ProductSnapshot.VariantSnapshot::status)
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple("CAM-P", "ACTIVE"),
                        org.assertj.core.api.Assertions.tuple("CAM-M", "INACTIVE"));
    }
}