-- V1: pedido do cliente. Nasce PENDING; o estado só anda para RESERVED ou REJECTED.
-- O banco é a última linha de defesa: não aceita um status que o código não conheça.

CREATE TABLE tb_order (
                          id          uuid                         NOT NULL,
                          created_at  timestamp(6) with time zone  NOT NULL,
                          updated_at  timestamp(6) with time zone  NOT NULL,
                          version     bigint                       NOT NULL,
                          tenant_id   uuid                         NOT NULL,
                          status      varchar(20)                  NOT NULL,
                          CONSTRAINT pk_order PRIMARY KEY (id),
                          CONSTRAINT ck_order_status CHECK (status IN ('PENDING', 'RESERVED', 'REJECTED'))
);