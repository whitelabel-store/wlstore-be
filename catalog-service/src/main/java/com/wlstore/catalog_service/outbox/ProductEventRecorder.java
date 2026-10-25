package com.wlstore.catalog_service.outbox;

import com.wlstore.catalog_service.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Grava na outbox o evento de um produto. Deve rodar DENTRO da transação que salvou o produto,
 * por isso MANDATORY: sem transação aberta, falha em vez de gravar o evento num commit separado.
 */

@Component
@RequiredArgsConstructor
public class ProductEventRecorder {
    public static final String AGGREGATE_TYPE = "PRODUCT";
    public static final String PRODUCT_CREATED = "product.created";
    public static final String PRODUCT_UPDATED = "product.updated";
    public static final String PRODUCT_DELETED = "product.deleted";

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String eventType, Product product) {
        ProductSnapshot snapshot = ProductSnapshot.from(product);
        String payload = objectMapper.writeValueAsString(snapshot);

        repository.save(OutboxEvent.of(
                product.getTenantId(),
                AGGREGATE_TYPE,
                product.getId(),
                eventType,
                payload));
    }
}
