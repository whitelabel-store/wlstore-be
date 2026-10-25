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
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ProductPriceListingIntegrationTest extends AbstractIntegrationTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @Autowired
    ProductService productService;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    VariantPriceRepository variantPriceRepository;

    // ---------- C3a: show the "from" price ----------

    @Test
    void listingShowsTheLowestVariantPriceOfEachProduct() {
        UUID tenant = UUID.randomUUID();
        UUID category = newCategory(tenant);

        ProductResponseDTO phone = newProduct(tenant, category, "Redmi 18", "PH-128", "PH-256");
        ProductResponseDTO phoneCase = newProduct(tenant, category, "Capa", "CASE-1");

        priceIt(tenant, phone.id(), "PH-256", 159900L);
        priceIt(tenant, phone.id(), "PH-128", 129900L);
        priceIt(tenant, phoneCase.id(), "CASE-1", 4990L);

        PageResponseDTO<ProductResponseDTO> page = list(tenant, priceBetween(null, null), PageRequest.of(0, 10));

        assertThat(page.data())
                .extracting(ProductResponseDTO::title, ProductResponseDTO::priceFromCents)
                .containsExactlyInAnyOrder(
                        tuple("Redmi 18", 129900L),
                        tuple("Capa", 4990L));
    }

    @Test
    void variantsWithoutPriceDoNotCountTowardsTheFromPrice() {
        UUID tenant = UUID.randomUUID();
        UUID category = newCategory(tenant);

        ProductResponseDTO phone = newProduct(tenant, category, "Redmi 18", "PH-128", "PH-256");
        priceIt(tenant, phone.id(), "PH-256", 159900L);

        PageResponseDTO<ProductResponseDTO> page = list(tenant, priceBetween(null, null), PageRequest.of(0, 10));

        assertThat(page.data())
                .singleElement()
                .extracting(ProductResponseDTO::priceFromCents)
                .isEqualTo(159900L);
    }

    @Test
    void productWithoutAnyPriceHasNullFromPriceNotZero() {
        UUID tenant = UUID.randomUUID();
        UUID category = newCategory(tenant);

        newProduct(tenant, category, "Sem preco", "NOPRICE-1");

        PageResponseDTO<ProductResponseDTO> page = list(tenant, priceBetween(null, null), PageRequest.of(0, 10));

        assertThat(page.data())
                .singleElement()
                .extracting(ProductResponseDTO::priceFromCents)
                .isNull();
    }

    // ---------- C3a2: the "de/por" comes from the SAME (cheapest) variant ----------

    @Test
    void listingShowsTheOriginalPriceOfTheCheapestVariant() {
        UUID tenant = UUID.randomUUID();
        UUID category = newCategory(tenant);

        ProductResponseDTO phone = newProduct(tenant, category, "Redmi 18", "PH-128", "PH-256");
        priceIt(tenant, phone.id(), "PH-128", 129_900L, 149_900L);
        priceIt(tenant, phone.id(), "PH-256", 159_900L, 199_900L);

        PageResponseDTO<ProductResponseDTO> page = list(tenant, priceBetween(null, null), PageRequest.of(0, 10));

        assertThat(page.data())
                .singleElement()
                .extracting(ProductResponseDTO::priceFromCents, ProductResponseDTO::originalPriceFromCents)
                .containsExactly(129_900L, 149_900L);
    }

    @Test
    void originalPriceIsNullWhenTheCheapestVariantHasNoDiscountEvenIfAnotherOneDoes() {
        UUID tenant = UUID.randomUUID();
        UUID category = newCategory(tenant);

        ProductResponseDTO phone = newProduct(tenant, category, "Redmi 18", "PH-128", "PH-256");
        priceIt(tenant, phone.id(), "PH-128", 129_900L);
        priceIt(tenant, phone.id(), "PH-256", 159_900L, 199_900L);

        PageResponseDTO<ProductResponseDTO> page = list(tenant, priceBetween(null, null), PageRequest.of(0, 10));

        assertThat(page.data())
                .singleElement()
                .extracting(ProductResponseDTO::priceFromCents, ProductResponseDTO::originalPriceFromCents)
                .containsExactly(129_900L, null);
    }

    // ---------- C3b: filter by price range (on the same "from" price the listing shows) ----------

    @Test
    void minPriceKeepsOnlyProductsWhoseFromPriceIsAtLeastThat() {
        UUID tenant = seedPhoneCaseAndUnpriced();

        PageResponseDTO<ProductResponseDTO> page = list(tenant, priceBetween(100_000L, null), PageRequest.of(0, 10));

        assertThat(page.data()).extracting(ProductResponseDTO::title).containsExactly("Redmi 18");
    }

    @Test
    void maxPriceKeepsOnlyProductsWhoseFromPriceIsAtMostThat() {
        UUID tenant = seedPhoneCaseAndUnpriced();

        PageResponseDTO<ProductResponseDTO> page = list(tenant, priceBetween(null, 50_000L), PageRequest.of(0, 10));

        assertThat(page.data()).extracting(ProductResponseDTO::title).containsExactly("Capa");
    }

    @Test
    void rangeKeepsBothEndsAndNeverReturnsProductsWithoutPrice() {
        UUID tenant = seedPhoneCaseAndUnpriced();

        PageResponseDTO<ProductResponseDTO> page = list(tenant, priceBetween(4_990L, 129_900L), PageRequest.of(0, 10));

        assertThat(page.data()).extracting(ProductResponseDTO::title)
                .containsExactlyInAnyOrder("Redmi 18", "Capa");
    }

    @Test
    void filterUsesTheFromPriceNotTheMostExpensiveVariant() {
        UUID tenant = seedPhoneCaseAndUnpriced();

        // the phone has a 159900 variant, but its from-price is 129900: the shop window would say "from 1299"
        PageResponseDTO<ProductResponseDTO> page = list(tenant, priceBetween(140_000L, null), PageRequest.of(0, 10));

        assertThat(page.data()).isEmpty();
    }

    @Test
    void totalElementsAndPagesReflectThePriceFilter() {
        UUID tenant = seedPhoneCaseAndUnpriced();

        PageResponseDTO<ProductResponseDTO> page = list(tenant, priceBetween(4_990L, 129_900L), PageRequest.of(0, 1));

        assertThat(page.data()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    // ---------- helpers ----------

    /** phone: from 129900 (variants 129900 and 159900) | case: 4990 | one product without any price. */
    private UUID seedPhoneCaseAndUnpriced() {
        UUID tenant = UUID.randomUUID();
        UUID category = newCategory(tenant);

        ProductResponseDTO phone = newProduct(tenant, category, "Redmi 18", "PH-128", "PH-256");
        ProductResponseDTO phoneCase = newProduct(tenant, category, "Capa", "CASE-1");
        newProduct(tenant, category, "Sem preco", "NOPRICE-1");

        priceIt(tenant, phone.id(), "PH-128", 129_900L);
        priceIt(tenant, phone.id(), "PH-256", 159_900L);
        priceIt(tenant, phoneCase.id(), "CASE-1", 4_990L);

        return tenant;
    }

    private ProductFilterDTO priceBetween(Long minPriceCents, Long maxPriceCents) {
        return new ProductFilterDTO(null, null, null, null, null, null, minPriceCents, maxPriceCents);
    }

    private PageResponseDTO<ProductResponseDTO> list(UUID tenant, ProductFilterDTO filter, Pageable pageable) {
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
        priceIt(tenant, productId, sku, amountCents, null);
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
}