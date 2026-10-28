package com.wlstore.catalog_service.outbox;

import com.wlstore.catalog_service.AbstractIntegrationTest;
import com.wlstore.catalog_service.dto.CategoryRefDTO;
import com.wlstore.catalog_service.dto.ProductRequestDTO;
import com.wlstore.catalog_service.dto.ProductVariantRequestDTO;
import com.wlstore.catalog_service.model.Category;
import com.wlstore.catalog_service.repository.CategoryRepository;
import com.wlstore.catalog_service.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class OutboxRelayIntegrationTest extends AbstractIntegrationTest {

    private static final String TEST_QUEUE = "test.catalog.events";

    @Autowired
    ProductService productService;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @Autowired
    OutboxEventPublisher publisher;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    AmqpAdmin amqpAdmin;

    @Autowired
    TopicExchange catalogEventsExchange;

    // Outros testes de integração (listagem, ordenação) criam produtos com commit
    // no mesmo banco e deixam eventos no outbox; aqui só contam os deste teste.
    // A classe é @Transactional: a limpeza é desfeita no fim de cada teste.
    @BeforeEach
    void emptyOutbox() {
        outboxEventRepository.deleteAllInBatch();
    }

    @Test
    void createdProductIsPublishedToRabbitAndMarkedAsPublished() {
        // Sem fila ligada ao exchange o RabbitMQ descarta a mensagem: esta fila faz o papel de um consumidor.
        Queue queue = new Queue(TEST_QUEUE, false, false, true);
        amqpAdmin.declareExchange(catalogEventsExchange);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(BindingBuilder.bind(queue).to(catalogEventsExchange).with("product.*"));
        amqpAdmin.purgeQueue(TEST_QUEUE, false);

        Category category = categoryRepository.saveAndFlush(root("Roupas", "roupas"));
        productService.create(new ProductRequestDTO(
                "Camiseta", "Marca", "Descrição", "ACTIVE",
                new CategoryRefDTO(category.getId()),
                List.of(new ProductVariantRequestDTO("CAM-P", "P", null, null, null, null)),
                List.of()));

        // O relayer agendado está desligado nos testes; aqui o chamamos na mão, na transação do teste.
        new OutboxRelayer(outboxEventRepository, publisher).relayPending();

        Message message = rabbitTemplate.receive(TEST_QUEUE, 5000);

        assertThat(message).as("mensagem recebida na fila").isNotNull();
        assertThat(message.getMessageProperties().getReceivedRoutingKey()).isEqualTo("product.created");
        assertThat(new String(message.getBody(), StandardCharsets.UTF_8)).contains("CAM-P");

        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getPublishedAt()).isNotNull();
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo(events.get(0).getId().toString());
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