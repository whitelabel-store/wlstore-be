-- V3: outbox de eventos do catálogo.
-- O evento é gravado na mesma transação do produto; um agendador publica no RabbitMQ depois.

CREATE TABLE tb_outbox_event (
                                 id              uuid                         NOT NULL,
                                 tenant_id       uuid                         NOT NULL,
                                 aggregate_type  varchar(50)                  NOT NULL,
                                 aggregate_id    uuid                         NOT NULL,
                                 event_type      varchar(100)                 NOT NULL,
                                 payload         jsonb                        NOT NULL,
                                 created_at      timestamp(6) with time zone  NOT NULL,
                                 published_at    timestamp(6) with time zone,
                                 CONSTRAINT pk_outbox_event PRIMARY KEY (id)
);

-- O agendador só procura o que ainda não foi publicado, na ordem em que foi criado.
CREATE INDEX idx_outbox_unpublished
    ON tb_outbox_event (created_at)
    WHERE published_at IS NULL;