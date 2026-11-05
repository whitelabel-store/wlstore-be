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
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductStockFilterIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ProductService productService;

    @Autowired
    CategoryRepository categoryRepository;

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
    void inStockFilterKeepsOnlyProductsWithUnitsAvailable() {
        UUID tenant = seed();

        PageResponseDTO<ProductResponseDTO> page = list(tenant, onlyInStock(), PageRequest.of(0, 10, byTitle()));

        // "Esgotado" (tudo zerado) e "Sem info" (sem linha de estoque) ficam de fora
        assertThat(page.data()).extracting(ProductResponseDTO::title)
                .containsExactly("Misto", "Redmi 18");
    }

    @Test
    void withoutTheFilterEveryProductIsListed() {
        UUID tenant = seed();

        assertThat(list(tenant, noFilter(), PageRequest.of(0, 10, byTitle())).data())
                .extracting(ProductResponseDTO::title)
                .containsExactly("Esgotado", "Misto", "Redmi 18", "Sem info");
    }

    @Test
    void filterFalseMeansNoFilterNotSoldOutOnly() {
        UUID tenant = seed();

        ProductFilterDTO inStockFalse =
                new ProductFilterDTO(null, null, null, null, null, null, null, null, false);

        assertThat(list(tenant, inStockFalse, PageRequest.of(0, 10, byTitle())).data())
                .extracting(ProductResponseDTO::title)
                .containsExactly("Esgotado", "Misto", "Redmi 18", "Sem info");
    }

    @Test
    void theFilterIsAppliedBeforePaginationSoPagesAreFullAndTheTotalIsRight() {
        UUID tenant = seed();

        PageResponseDTO<ProductResponseDTO> first = list(tenant, onlyInStock(), PageRequest.of(0, 1, byTitle()));
        PageResponseDTO<ProductResponseDTO> second = list(tenant, onlyInStock(), PageRequest.of(1, 1, byTitle()));

        assertThat(first.data()).extracting(ProductResponseDTO::title).containsExactly("Misto");
        assertThat(second.data()).extracting(ProductResponseDTO::title).containsExactly("Redmi 18");
        assertThat(first.totalElements()).isEqualTo(2);
        assertThat(first.totalPages()).isEqualTo(2);
    }

    // ---------- seed ----------

    /**
     * Redmi 18: uma variante com saldo (entra)
     * Misto: uma variante zerada e outra com saldo (entra)
     * Esgotado: tudo zerado (fora)
     * Sem info: nenhuma linha de estoque (fora)
     */
    private UUID seed() {
        UUID tenant = UUID.randomUUID();
        UUID category = newCategory(tenant);

        newProduct(tenant, category, "Redmi 18", "PH-128");
        newProduct(tenant, category, "Misto", "MIX-1", "MIX-2");
        newProduct(tenant, category, "Esgotado", "OUT-1", "OUT-2");
        newProduct(tenant, category, "Sem info", "NOINFO-1");

        stockIt(tenant, "PH-128", 7);
        stockIt(tenant, "MIX-1", 0);
        stockIt(tenant, "MIX-2", 3);
        stockIt(tenant, "OUT-1", 0);
        stockIt(tenant, "OUT-2", 0);

        return tenant;
    }

    // ---------- helpers ----------

    private ProductFilterDTO onlyInStock() {
        return new ProductFilterDTO(null, null, null, null, null, null, null, null, true);
    }

    private ProductFilterDTO noFilter() {
        return new ProductFilterDTO(null, null, null, null, null, null, null, null, null);
    }

    private Sort byTitle() {
        return Sort.by("title").ascending();
    }

    private PageResponseDTO<ProductResponseDTO> list(UUID tenant, ProductFilterDTO filter,
                                                     PageRequest pageable) {
        return TenantContext.callWithTenant(tenant, () -> productService.findAll(filter, pageable));
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