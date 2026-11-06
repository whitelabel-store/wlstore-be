-- V4: um registro por pedido que reservou estoque. É o que torna a reserva idempotente:
-- o mesmo pedido repetido (retry depois de um timeout) encontra a linha e não reserva de novo.
-- A unicidade por (tenant, pedido) é a última linha de defesa contra duas chamadas simultâneas.

CREATE TABLE tb_reservation (
                                id          uuid                         NOT NULL,
                                created_at  timestamp(6) with time zone  NOT NULL,
                                updated_at  timestamp(6) with time zone  NOT NULL,
                                version     bigint                       NOT NULL,
                                tenant_id   uuid                         NOT NULL,
                                order_id    uuid                         NOT NULL,
                                CONSTRAINT pk_reservation PRIMARY KEY (id),
                                CONSTRAINT uk_reservation_tenant_order UNIQUE (tenant_id, order_id)
);