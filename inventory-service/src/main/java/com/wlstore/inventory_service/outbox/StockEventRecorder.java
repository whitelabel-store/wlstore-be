package com.wlstore.inventory_service.outbox;

import com.wlstore.common.outbox.OutboxEvent;
import com.wlstore.common.outbox.OutboxEventRepository;
import com.wlstore.inventory_service.model.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes the stock.changed event of a stock row to the outbox. It must run INSIDE the transaction that
 * changed the stock, hence MANDATORY: without an open transaction it fails instead of writing the event
 * in a separate commit.
 */
@Component
@RequiredArgsConstructor
public class StockEventRecorder {
    public static final String AGGREGATE_TYPE = "STOCK";
    public static final String STOCK_CHANGED = "stock.changed";

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Stock stock) {
        StockChangedMessage message = new StockChangedMessage(
                stock.getTenantId(),
                stock.getSku(),
                stock.getQuantityOnHand(),
                stock.getQuantityReserved(),
                stock.available(),
                stock.getVersion()
        );

        repository.save(OutboxEvent.of(
                stock.getTenantId(),
                AGGREGATE_TYPE,
                stock.getId(),
                STOCK_CHANGED,
                objectMapper.writeValueAsString(message)
        ));
    }
}
