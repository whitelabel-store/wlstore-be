-- V8: read model da disponibilidade de estoque. O dono do estoque é o inventory-service; aqui fica uma
-- cópia, atualizada por eventos stock.changed, para a vitrine poder mostrar e filtrar por disponibilidade.
-- stock_version é a version que veio no evento: só se aceita um evento com version MAIOR que a guardada.

CREATE TABLE tb_variant_stock (
                                  id            uuid                         NOT NULL,
                                  created_at    timestamp(6) with time zone  NOT NULL,
                                  updated_at    timestamp(6) with time zone  NOT NULL,
                                  version       bigint                       NOT NULL,
                                  tenant_id     uuid                         NOT NULL,
                                  sku           varchar(64)                  NOT NULL,
                                  available     integer                      NOT NULL,
                                  stock_version bigint                       NOT NULL,
                                  CONSTRAINT pk_variant_stock PRIMARY KEY (id),
                                  CONSTRAINT uk_variant_stock_tenant_sku UNIQUE (tenant_id, sku),
                                  CONSTRAINT ck_variant_stock_available CHECK (available >= 0)
);