-- V4: inbox do catálogo. Cada mensagem recebida é anotada aqui na mesma transação do trabalho,
-- então uma entrega repetida (o RabbitMQ entrega pelo menos uma vez) é reconhecida e ignorada.

CREATE TABLE tb_processed_event (
                                    message_id   uuid                         NOT NULL,
                                    processed_at timestamp(6) with time zone  NOT NULL,

                                    CONSTRAINT pk_processed_event PRIMARY KEY (message_id)
);