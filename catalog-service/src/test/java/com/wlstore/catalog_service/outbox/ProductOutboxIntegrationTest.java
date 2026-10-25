package com.wlstore.catalog_service.outbox;

import com.wlstore.catalog_service.AbstractIntegrationTest;
import com.wlstore.catalog_service.dto.CategoryRefDTO;
import com.wlstore.catalog_service.dto.ProductRequestDTO;
import com.wlstore.catalog_service.dto.ProductResponseDTO;
import com.wlstore.catalog_service.dto.ProductVariantRequestDTO;
import com.wlstore.catalog_service.model.Category;
import com.wlstore.catalog_service.repository.CategoryRepository;
import com.wlstore.catalog_service.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ProductOutboxIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ProductService productService;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @Test
    void createWritesProductCreatedEventToOutbox() {
        Category category = categoryRepository.saveAndFlush(root("Roupas", "roupas"));

        ProductResponseDTO created = productService.create(requestWith(category, "CAM-P", "P"));

        List<OutboxEvent> events = outboxEventRepository.findAll();

        assertThat(events).hasSize(1);
        OutboxEvent event = events.get(0);
        assertThat(event.getEventType()).isEqualTo("product.created");
        assertThat(event.getAggregateId()).isEqualTo(created.id());
        assertThat(event.getPayload()).contains("CAM-P");
        assertThat(event.getPublishedAt()).isNull();
    }

    @Test
    void updateWritesProductUpdatedEventWithCurrentSnapshot() {
        Category category = categoryRepository.saveAndFlush(root("Roupas", "roupas"));
        ProductResponseDTO created = productService.create(requestWith(category, "CAM-P", "P"));

        // troca a variante CAM-P por CAM-M
        productService.update(created.id(), requestWith(category, "CAM-M", "M"));

        List<OutboxEvent> events = outboxEventRepository.findAll();

        assertThat(events)
                .extracting(OutboxEvent::getEventType)
                .containsExactlyInAnyOrder("product.created", "product.updated");

        OutboxEvent updated = events.stream()
                .filter(e -> e.getEventType().equals("product.updated"))
                .findFirst()
                .orElseThrow();

        assertThat(updated.getAggregateId()).isEqualTo(created.id());
        assertThat(updated.getPayload()).contains("CAM-M");
        assertThat(updated.getPayload()).doesNotContain("CAM-P");
    }

    @Test
    void deleteWritesProductDeletedEventWithLastSnapshot() {
        Category category = categoryRepository.saveAndFlush(root("Roupas", "roupas"));
        ProductResponseDTO created = productService.create(requestWith(category, "CAM-P", "P"));

        productService.delete(created.id());

        List<OutboxEvent> events = outboxEventRepository.findAll();

        assertThat(events)
                .extracting(OutboxEvent::getEventType)
                .containsExactlyInAnyOrder("product.created", "product.deleted");

        OutboxEvent deleted = events.stream()
                .filter(e -> e.getEventType().equals("product.deleted"))
                .findFirst()
                .orElseThrow();

        assertThat(deleted.getAggregateId()).isEqualTo(created.id());
        // o último snapshot diz quais SKUs deixaram de existir
        assertThat(deleted.getPayload()).contains("CAM-P");
    }

    private ProductRequestDTO requestWith(Category category, String sku, String variantName) {
        return new ProductRequestDTO(
                "Camiseta", "Marca", "Descrição", "ACTIVE",
                new CategoryRefDTO(category.getId()),
                List.of(new ProductVariantRequestDTO(sku, variantName, null, null, null, null)),
                List.of());
    }

    private Category root(String title, String slug) {
        Category category = Category.builder()
                .title(title)
                .slug(slug)
                .build();
        category.refreshPath();
        return category;
    }
}