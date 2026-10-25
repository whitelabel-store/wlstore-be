-- V5: read model do preço. O dono do preço é o pricing; aqui fica uma cópia, atualizada por eventos
-- price.changed, para a listagem poder mostrar, filtrar e ordenar por preço na mesma consulta.

CREATE TABLE tb_variant_price (
                                  id                    uuid                         NOT NULL,
                                  created_at            timestamp(6) with time zone  NOT NULL,
                                  updated_at            timestamp(6) with time zone  NOT NULL,
                                  version               bigint                       NOT NULL,
                                  tenant_id             uuid                         NOT NULL,
                                  product_id            uuid                         NOT NULL,
                                  sku                   varchar(64)                  NOT NULL,
                                  amount_cents          bigint                       NOT NULL,
                                  original_amount_cents bigint,
                                  valid_from            timestamp(6) with time zone  NOT NULL,
                                  CONSTRAINT pk_variant_price PRIMARY KEY (id),
                                  CONSTRAINT uk_variant_price_tenant_sku UNIQUE (tenant_id, sku),
                                  CONSTRAINT ck_variant_price_amount CHECK (amount_cents > 0)
);

CREATE INDEX idx_variant_price_product ON tb_variant_price (tenant_id, product_id);