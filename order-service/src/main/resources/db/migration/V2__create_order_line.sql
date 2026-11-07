-- V2: linhas do pedido. Registro histórico: o que o cliente pediu, SKU a SKU.
-- O banco barra quantidade inválida e linha sem pedido, mesmo que o código erre.

CREATE TABLE tb_order_line (
                               id          uuid                         NOT NULL,
                               created_at  timestamp(6) with time zone  NOT NULL,
                               updated_at  timestamp(6) with time zone  NOT NULL,
                               version     bigint                       NOT NULL,
                               tenant_id   uuid                         NOT NULL,
                               order_id    uuid                         NOT NULL,
                               sku         varchar(64)                  NOT NULL,
                               quantity    integer                      NOT NULL,
                               CONSTRAINT pk_order_line PRIMARY KEY (id),
                               CONSTRAINT fk_order_line_order FOREIGN KEY (order_id) REFERENCES tb_order (id),
                               CONSTRAINT ck_order_line_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX ix_order_line_tenant_order ON tb_order_line (tenant_id, order_id);