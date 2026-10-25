package com.wlstore.catalog_service.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProcessedEventInbox {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean registerIfFirstTime(UUID messageId) {
        int inserted = jdbcTemplate.update("""
                insert into tb_processed_event (message_id, processed_at)
                values (?, now())
                on conflict (message_id) do nothing
                """, messageId);

        return inserted == 1;
    }
}
