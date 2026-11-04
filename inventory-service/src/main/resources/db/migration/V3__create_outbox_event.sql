-- V3: outbox of inventory events.
-- The event is written in the same transaction as the stock change; a scheduler publishes it to RabbitMQ later.

CREATE TABLE tb_outbox_event (
    id              uuid                        NOT NULL,
    tenant_id       uuid                        NOT NULL,
    aggregate_type  varchar(50)                 NOT NULL,
    aggregate_id    uuid                        NOT NULL,
    event_type      varchar(100)                NOT NULL,
    payload         jsonb                       NOT NULL,
    created_at      timestamp(6) with time zone NOT NULL,
    published_at    timestamp(6) with time zone,
    CONSTRAINT pk_outbox_event PRIMARY KEY (id)
);

-- The relayer only looks for what has not been published yet, in creation order.
CREATE INDEX idx_outbox_unpublished
    ON tb_outbox_event (created_at)
    WHERE published_at IS NULL;