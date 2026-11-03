package com.wlstore.common.messaging;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Inbox for idempotent consumers: remembers which messageIds were already handled.
 * <p>
 * Contract: the service's own database must have the table {@code tb_processed_event}
 * (message_id uuid primary key, processed_at timestamptz), created by the service's migration.
 * <p>
 * It must run inside the same transaction as the work it protects, hence MANDATORY:
 * if the work fails, the registration is rolled back together and the retry processes the message again.
 */
public class ProcessedEventInbox {

    private final JdbcTemplate jdbcTemplate;

    public ProcessedEventInbox(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** @return true if this is the first time the message is seen; false if it is a duplicate */
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
