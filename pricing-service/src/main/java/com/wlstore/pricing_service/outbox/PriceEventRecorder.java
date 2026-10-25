package com.wlstore.pricing_service.outbox;

import com.wlstore.pricing_service.model.Price;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * Grava na outbox o evento de um preço. Deve rodar DENTRO da transação que salvou o preço,
 * por isso MANDATORY: sem transação aberta, falha em vez de gravar o evento num commit separado.
 */
@Component
@RequiredArgsConstructor
public class PriceEventRecorder {

    public static final String AGGREGATE_TYPE = "PRICE";
    public static final String PRICE_CHANGED = "price.changed";

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Price price, UUID productId) {
        PriceChangedMessage message = new PriceChangedMessage(
                price.getTenantId(),
                productId,
                price.getSku(),
                price.getAmountCents(),
                price.getOriginalAmountCents(),
                price.getValidFrom());

        String payload = objectMapper.writeValueAsString(message);

        repository.save(OutboxEvent.of(
                price.getTenantId(),
                AGGREGATE_TYPE,
                price.getId(),
                PRICE_CHANGED,
                payload));
    }
}
