-- V1: saldo de estoque por SKU. O banco é a última linha de defesa: nunca aceita um estado impossível,
-- mesmo que duas requisições passem juntas pela validação do código.

CREATE TABLE tb_stock (
                          id                 uuid                         NOT NULL,
                          created_at         timestamp(6) with time zone  NOT NULL,
                          updated_at         timestamp(6) with time zone  NOT NULL,
                          version            bigint                       NOT NULL,
                          tenant_id          uuid                         NOT NULL,
                          sku                varchar(64)                  NOT NULL,
                          quantity_on_hand   integer                      NOT NULL,
                          quantity_reserved  integer                      NOT NULL,
                          CONSTRAINT pk_stock PRIMARY KEY (id),
                          CONSTRAINT uk_stock_tenant_sku UNIQUE (tenant_id, sku),
                          CONSTRAINT ck_stock_on_hand_non_negative CHECK (quantity_on_hand >= 0),
                          CONSTRAINT ck_stock_reserved_non_negative CHECK (quantity_reserved >= 0),
                          CONSTRAINT ck_stock_reserved_within_on_hand CHECK (quantity_reserved <= quantity_on_hand)
);