package com.wlstore.catalog_service.service;

import com.wlstore.catalog_service.AbstractIntegrationTest;
import com.wlstore.catalog_service.dto.CategoryRefDTO;
import com.wlstore.catalog_service.dto.ProductRequestDTO;
import com.wlstore.catalog_service.dto.ProductResponseDTO;
import com.wlstore.catalog_service.dto.ProductVariantRequestDTO;
import com.wlstore.catalog_service.model.Category;
import com.wlstore.catalog_service.model.VariantPrice;
import com.wlstore.catalog_service.model.VariantStock;
import com.wlstore.catalog_service.repository.CategoryRepository;
import com.wlstore.catalog_service.repository.VariantPriceRepository;
import com.wlstore.catalog_service.repository.VariantStockRepository;
import com.wlstore.common.outbox.OutboxEventRepository;
import com.wlstore.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductDetailIntegrationTest extends AbstractIntegrationTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @Autowired
    ProductService productService;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    VariantPriceRepository variantPriceRepository;

    @Autowired
    VariantStockRepository variantStockRepository;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    // Esta classe commita e o outbox é global.
    @AfterEach
    void cleanOutbox() {
        outboxEventRepository.deleteAllInBatch();
    }

    @Test
    void detailShowsTheFromPriceItsOriginalAndTheAvailability() {
        UUID tenant = UUID.randomUUID();
        UUID category = newCategory(tenant);
        ProductResponseDTO phone = newProduct(tenant, category, "Redmi 18", "PH-128", "PH-256");

        priceIt(tenant, phone.id(), "PH-128", 129_900L, 149_900L);
        priceIt(tenant, phone.id(), "PH-256", 159_900L, null);
        stockIt(tenant, "PH-128", 0);
        stockIt(tenant, "PH-256", 5);

        ProductResponseDTO detail = detail(tenant, phone.id());

        // o preço "a partir de" e o "de" vêm da mesma linha (a mais barata)
        assertThat(detail.priceFromCents()).isEqualTo(129_900L);
        assertThat(detail.originalPriceFromCents()).isEqualTo(149_900L);
        // a variante barata está zerada, mas a outra tem saldo: o produto está em estoque
        assertThat(detail.inStock()).isTrue();
    }

    @Test
    void detailOfASoldOutProductIsExplicitlyOutOfStock() {
        UUID tenant = UUID.randomUUID();
        UUID category = newCategory(tenant);
        ProductResponseDTO phoneCase = newProduct(tenant, category, "Capa", "CASE-1");

        priceIt(tenant, phoneCase.id(), "CASE-1", 4_990L, null);
        stockIt(tenant, "CASE-1", 0);

        ProductResponseDTO detail = detail(tenant, phoneCase.id());

        assertThat(detail.priceFromCents()).isEqualTo(4_990L);
        assertThat(detail.inStock()).isFalse();
    }

    @Test
    void detailWithoutPriceOrStockInformationKeepsThemNull() {
        UUID tenant = UUID.randomUUID();
        UUID category = newCategory(tenant);
        ProductResponseDTO cable = newProduct(tenant, category, "Cabo", "CABLE-1");

        ProductResponseDTO detail = detail(tenant, cable.id());

        assertThat(detail.title()).isEqualTo("Cabo");
        assertThat(detail.priceFromCents()).isNull();
        assertThat(detail.originalPriceFromCents()).isNull();
        assertThat(detail.inStock()).isNull();
    }

    // ---------- helpers ----------

    private ProductResponseDTO detail(UUID tenant, UUID productId) {
        return TenantContext.callWithTenant(tenant, () -> productService.findById(productId));
    }

    private UUID newCategory(UUID tenant) {
        return TenantContext.callWithTenant(tenant, () -> {
            String name = "cat-" + UUID.randomUUID().toString().substring(0, 8);
            Category category = Category.builder().title(name).slug(name).build();
            category.refreshPath();
            return categoryRepository.saveAndFlush(category).getId();
        });
    }

    private ProductResponseDTO newProduct(UUID tenant, UUID categoryId, String title, String... skus) {
        List<ProductVariantRequestDTO> variants = Arrays.stream(skus)
                .map(sku -> new ProductVariantRequestDTO(sku, "Variant " + sku, null, null, null, null))
                .toList();

        ProductRequestDTO request = new ProductRequestDTO(
                title, "Brand", "Description", "ACTIVE",
                new CategoryRefDTO(categoryId), variants, null);

        return TenantContext.callWithTenant(tenant, () -> productService.create(request));
    }

    private void priceIt(UUID tenant, UUID productId, String sku, long amountCents, Long originalAmountCents) {
        TenantContext.runWithTenant(tenant, () -> variantPriceRepository.saveAndFlush(
                VariantPrice.builder()
                        .productId(productId)
                        .sku(sku)
                        .amountCents(amountCents)
                        .originalAmountCents(originalAmountCents)
                        .validFrom(T0)
                        .build()));
    }

    private void stockIt(UUID tenant, String sku, int available) {
        TenantContext.runWithTenant(tenant, () -> variantStockRepository.saveAndFlush(
                VariantStock.builder()
                        .sku(sku)
                        .available(available)
                        .stockVersion(1L)
                        .build()));
    }
}