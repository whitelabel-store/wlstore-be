package com.wlstore.common.config;

import com.wlstore.common.outbox.OutboxEvent;
import com.wlstore.common.outbox.OutboxEventPublisher;
import com.wlstore.common.outbox.OutboxEventRepository;
import com.wlstore.common.outbox.OutboxRelayer;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Transactional outbox for services that opt in with wlstore.outbox.enabled=true.
 * <ul>
 *   <li>Registers the outbox entity and repository. Opt-in because the service's database must have the
 *       tb_outbox_event table (its own migration): with ddl-auto=validate, an entity without a table
 *       stops the application from starting.</li>
 *   <li>Publishes pending events to the exchange named in wlstore.outbox.exchange.</li>
 *   <li>The scheduled relayer can be turned off with wlstore.outbox.relay.enabled=false (tests do).</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass(EntityManager.class)
@ConditionalOnProperty(prefix = "wlstore.outbox", name = "enabled", havingValue = "true")
@AutoConfigurationPackage(basePackageClasses = OutboxEvent.class)
public class WlstoreOutboxAutoConfiguration {

    /** Nested so RabbitTemplate is only loaded when the (optional) AMQP library is present. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RabbitTemplate.class)
    @EnableScheduling
    static class OutboxPublishingConfiguration {

        @Bean
        @ConditionalOnMissingBean
        OutboxEventPublisher outboxEventPublisher(RabbitTemplate rabbitTemplate,
                                                  @Value("${wlstore.outbox.exchange}") String exchange) {
            return new OutboxEventPublisher(rabbitTemplate, exchange);
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "wlstore.outbox.relay", name = "enabled", havingValue = "true",
                matchIfMissing = true)
        OutboxRelayer outboxRelayer(OutboxEventRepository repository, OutboxEventPublisher publisher) {
            return new OutboxRelayer(repository, publisher);
        }
    }
}