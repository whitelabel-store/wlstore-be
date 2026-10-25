package com.wlstore.pricing_service.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.wlstore.pricing_service.AbstractIntegrationTest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProcessedEventInboxTest extends AbstractIntegrationTest {

    @Autowired
    ProcessedEventInbox inbox;

    @Test
    void firstDeliveryIsRegisteredAndDuplicateIsRejected() {
        UUID messageId = UUID.randomUUID();

        assertThat(inbox.registerIfFirstTime(messageId)).isTrue();
        assertThat(inbox.registerIfFirstTime(messageId)).isFalse();
    }

    @Test
    void differentMessagesAreIndependent() {
        assertThat(inbox.registerIfFirstTime(UUID.randomUUID())).isTrue();
        assertThat(inbox.registerIfFirstTime(UUID.randomUUID())).isTrue();
    }
}