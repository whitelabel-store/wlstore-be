package com.wlstore.catalog_service.service.impl;

import com.wlstore.catalog_service.dto.CategoryRefDTO;
import com.wlstore.catalog_service.dto.ProductImageRequestDTO;
import com.wlstore.catalog_service.dto.ProductRequestDTO;
import com.wlstore.catalog_service.dto.ProductVariantRequestDTO;
import com.wlstore.catalog_service.mapper.ProductMapper;
import com.wlstore.catalog_service.repository.CategoryRepository;
import com.wlstore.catalog_service.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    ProductRepository productRepository;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    ProductMapper productMapper;

    @InjectMocks
    ProductServiceImpl service;

    @Test
    void create_rejectsDuplicateVariantSku() {
        ProductRequestDTO request = requestWith(
                List.of(variant("SKU-1"), variant("SKU-1")),
                List.of()
        );

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate variant SKU: SKU-1");

        verifyNoInteractions(productRepository, categoryRepository, productMapper);
    }

    @Test
    void create_rejectsMoreThanOnePrimaryImage() {
        ProductRequestDTO request = requestWith(
                List.of(),
                List.of(image(true, null), image(true, null))
        );

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only one image can be primary");

        verifyNoInteractions(productRepository, categoryRepository, productMapper);
    }

    @Test
    void create_rejectsImageReferencingUnknownVariantSku() {
        ProductRequestDTO request = requestWith(
                List.of(variant("SKU-1")),
                List.of(image(false, "SKU-UNKNOWN"))
        );

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Image references unknown variant SKU: SKU-UNKNOWN");

        verifyNoInteractions(productRepository, categoryRepository, productMapper);
    }

    private ProductVariantRequestDTO variant(String sku) {
        return new ProductVariantRequestDTO(sku, "Variant " + sku, null, null, null, null);
    }

    private ProductImageRequestDTO image(boolean primaryImage, String variantSku) {
        return new ProductImageRequestDTO("http://example.com/image.png", null, null, primaryImage, variantSku);
    }

    private ProductRequestDTO requestWith(List<ProductVariantRequestDTO> variants,
                                          List<ProductImageRequestDTO> images) {
        return new ProductRequestDTO(
                "Title", "Brand", "Description", "ACTIVE",
                new CategoryRefDTO(UUID.randomUUID()),
                variants, images
        );
    }
}