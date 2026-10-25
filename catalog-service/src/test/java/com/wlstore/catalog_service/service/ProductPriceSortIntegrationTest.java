package com.wlstore.catalog_service.service;

import com.wlstore.catalog_service.AbstractIntegrationTest;
import com.wlstore.catalog_service.dto.CategoryRefDTO;
import com.wlstore.catalog_service.dto.ProductFilterDTO;
import com.wlstore.catalog_service.dto.ProductRequestDTO;
import com.wlstore.catalog_service.dto.ProductResponseDTO;
import com.wlstore.catalog_service.dto.ProductVariantRequestDTO;
import com.wlstore.catalog_service.model.Category;
import com.wlstore.catalog_service.model.VariantPrice;
import com.wlstore.catalog_service.repository.CategoryRepository;
import com.wlstore.catalog_service.repository.VariantPriceRepository;
import com.wlstore.common.tenant.TenantContext;
import com.wlstore.common.web.PageResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductPriceSortIntegrationTest extends AbstractIntegrationTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @Autowired
    ProductService productService;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    VariantPriceRepository variantPriceRepository;

    @Test
    void sortsByFromPriceAscendingWithUnpricedProductsLast() {
        UUID tenant = seed();

        PageResponseDTO<ProductResponseDTO> page =
                list(tenant, noFilter(), PageRequest.of(0, 10, Sort.by("priceFromCents").ascending()));

        assertThat(page.data()).extracting(ProductResponseDTO::title)
                .containsExactly("Capa", "Fone", "Redmi 18", "Sem preco");
    }

    @Test
    void sortsByFromPriceDescendingStillKeepingUnpricedProductsLast() {
        UUID tenant = seed();

        PageResponseDTO<ProductResponseDTO> page =
                list(tenant, noFilter(), PageRequest.of(0, 10, Sort.by("priceFromCents").descending()));

        assertThat(page.data()).extracting(ProductResponseDTO::title)
                .containsExactly("Redmi 18", "Fone", "Capa", "Sem preco");
    }

    @Test
    void priceSortIsAppliedBeforePaginationNotWithinEachPage() {
        UUID tenant = seed();
        Sort byPrice = Sort.by("priceFromCents").ascending();

        PageResponseDTO<ProductResponseDTO> first = list(tenant, noFilter(), PageRequest.of(0, 2, byPrice));
        PageResponseDTO<ProductResponseDTO> second = list(tenant, noFilter(), PageRequest.of(1, 2, byPrice));

        assertThat(first.data()).extracting(ProductResponseDTO::title).containsExactly("Capa", "Fone");
        assertThat(second.data()).extracting(ProductResponseDTO::title).containsExactly("Redmi 18", "Sem preco");
        assertThat(first.totalElements()).isEqualTo(4);
    }

    @Test
    void priceSortCombinesWithThePriceFilter() {
        UUID tenant = seed();

        PageResponseDTO<ProductResponseDTO> page = list(tenant,
                new ProductFilterDTO(null, null, null, null, null, null, 10_000L, null),
                PageRequest.of(0, 10, Sort.by("priceFromCents").descending()));

        assertThat(page.data()).extracting(ProductResponseDTO::title).containsExactly("Redmi 18", "Fone");
        assertThat(page.totalElements()).isEqualTo(2);
    }

    @Test
    void otherSortFieldsKeepWorking() {
        UUID tenant = seed();

        PageResponseDTO<ProductResponseDTO> page =
                list(tenant, noFilter(), PageRequest.of(0, 10, Sort.by("title").descending()));

        assertThat(page.data()).extracting(ProductResponseDTO::title)
                .containsExactly("Sem preco", "Redmi 18", "Fone", "Capa");
    }

    // ---------- helpers ----------

    /** Capa 4990 | Fone 29900 | Redmi 18 from 129900 (variants 129900 and 159900) | Sem preco has no price. */
    private UUID seed() {
        UUID tenant = UUID.randomUUID();
        UUID category = newCategory(tenant);

        ProductResponseDTO phone = newProduct(tenant, category, "Redmi 18", "PH-128", "PH-256");
        ProductResponseDTO phoneCase = newProduct(tenant, category, "Capa", "CASE-1");
        ProductResponseDTO headset = newProduct(tenant, category, "Fone", "FONE-1");
        newProduct(tenant, category, "Sem preco", "NOPRICE-1");

        priceIt(tenant, phone.id(), "PH-128", 129_900L);
        priceIt(tenant, phone.id(), "PH-256", 159_900L);
        priceIt(tenant, phoneCase.id(), "CASE-1", 4_990L);
        priceIt(tenant, headset.id(), "FONE-1", 29_900L);

        return tenant;
    }

    private ProductFilterDTO noFilter() {
        return new ProductFilterDTO(null, null, null, null, null, null, null, null);
    }

    private PageResponseDTO<ProductResponseDTO> list(UUID tenant, ProductFilterDTO filter,
                                                     org.springframework.data.domain.Pageable pageable) {
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

    private void priceIt(UUID tenant, UUID productId, String sku, long amountCents) {
        TenantContext.runWithTenant(tenant, () -> variantPriceRepository.saveAndFlush(
                VariantPrice.builder()
                        .productId(productId)
                        .sku(sku)
                        .amountCents(amountCents)
                        .validFrom(T0)
                        .build()));
    }
}