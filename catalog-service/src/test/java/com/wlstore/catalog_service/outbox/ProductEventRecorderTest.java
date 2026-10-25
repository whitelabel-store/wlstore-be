package com.wlstore.catalog_service.outbox;

import com.wlstore.catalog_service.enums.ActiveStatus;
import com.wlstore.catalog_service.enums.ProductStatus;
import com.wlstore.catalog_service.model.Product;
import com.wlstore.catalog_service.model.ProductVariant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductEventRecorderTest {

    @Mock
    OutboxEventRepository repository;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private ProductEventRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new ProductEventRecorder(repository, jsonMapper);
    }

    @Test
    void recordSavesSnapshotInOutbox() {
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

        recorder.record(ProductEventRecorder.PRODUCT_CREATED, product);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        OutboxEvent event = captor.getValue();

        assertThat(event.getTenantId()).isEqualTo(tenantId);
        assertThat(event.getAggregateType()).isEqualTo("PRODUCT");
        assertThat(event.getAggregateId()).isEqualTo(productId);
        assertThat(event.getEventType()).isEqualTo("product.created");

        // o payload gravado deve voltar a ser exatamente o snapshot do produto
        ProductSnapshot payload = jsonMapper.readValue(event.getPayload(), ProductSnapshot.class);
        assertThat(payload).isEqualTo(ProductSnapshot.from(product));
    }
}