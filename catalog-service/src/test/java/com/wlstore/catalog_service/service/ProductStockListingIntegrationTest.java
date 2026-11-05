package com.wlstore.catalog_service.service;

import com.wlstore.catalog_service.AbstractIntegrationTest;
import com.wlstore.catalog_service.dto.CategoryRefDTO;
import com.wlstore.catalog_service.dto.ProductFilterDTO;
import com.wlstore.catalog_service.dto.ProductRequestDTO;
import com.wlstore.catalog_service.dto.ProductResponseDTO;
import com.wlstore.catalog_service.dto.ProductVariantRequestDTO;
import com.wlstore.catalog_service.model.Category;
import com.wlstore.catalog_service.model.VariantStock;
import com.wlstore.catalog_service.repository.CategoryRepository;
import com.wlstore.catalog_service.repository.VariantStockRepository;
import com.wlstore.common.outbox.OutboxEventRepository;
import com.wlstore.common.tenant.TenantContext;
import com.wlstore.common.web.PageResponseDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ProductStockListingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ProductService productService;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    VariantStockRepository variantStockRepository;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    // Mesma razão do teste de preço: esta classe commita e o outbox é global.
    @AfterEach
    void cleanOutbox() {
        outboxEventRepository.deleteAllInBatch();
    }

    @Test
    void productIsInStockWhenAtLeastOneVariantHasUnitsAvailable() {
        UUID tenant = UUID.randomUUID();
        UUID category = newCategory(tenant);

        newProduct(tenant, category, "Redmi 18", "PH-128", "PH-256");
        stockIt(tenant, "PH-128", 0);
        stockIt(tenant, "PH-256", 5);

        assertThat(list(tenant).data())
                .extracting(ProductResponseDTO::title, ProductResponseDTO::inStock)
                .containsExactly(tuple("Redmi 18", true));
    }

    @Test
    void productIsOutOfStockWhenEveryVariantIsSoldOut() {
        UUID tenant = UUID.randomUUID();
        UUID category = newCategory(tenant);

        newProduct(tenant, category, "Capa", "CASE-1", "CASE-2");
        stockIt(tenant, "CASE-1", 0);
        stockIt(tenant, "CASE-2", 0);

        assertThat(list(tenant).data())
                .extracting(ProductResponseDTO::title, ProductResponseDTO::inStock)
                .containsExactly(tuple("Capa", false));
    }

    @Test
    void availabilityIsUnknownWhenNoVariantHasAStockRow() {
        UUID tenant = UUID.randomUUID();
        UUID category = newCategory(tenant);

        newProduct(tenant, category, "Cabo", "CABLE-1");

        assertThat(list(tenant).data())
                .extracting(ProductResponseDTO::title, ProductResponseDTO::inStock)
                .containsExactly(tuple("Cabo", null));
    }

    // ---------- helpers ----------

    private PageResponseDTO<ProductResponseDTO> list(UUID tenant) {
        return TenantContext.callWithTenant(tenant,
                () -> productService.findAll(
                        new ProductFilterDTO(null, null, null, null, null, null, null, null, null),
                        PageRequest.of(0, 10)));
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

    private void stockIt(UUID tenant, String sku, int available) {
        TenantContext.runWithTenant(tenant, () -> variantStockRepository.saveAndFlush(
                VariantStock.builder()
                        .sku(sku)
                        .available(available)
                        .stockVersion(1L)
                        .build()));
    }
}