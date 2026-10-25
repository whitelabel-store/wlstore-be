CREATE TABLE tb_price (
                          id                     uuid                         NOT NULL,
                          created_at             timestamp(6) with time zone  NOT NULL,
                          updated_at             timestamp(6) with time zone  NOT NULL,
                          version                bigint                       NOT NULL,
                          tenant_id              uuid                         NOT NULL,
                          sku                    varchar(64)                  NOT NULL,
                          amount_cents           bigint                       NOT NULL,
                          original_amount_cents  bigint,
                          valid_from             timestamp(6) with time zone  NOT NULL,
                          valid_to               timestamp(6) with time zone,

                          CONSTRAINT pk_price PRIMARY KEY (id),
                          CONSTRAINT ck_price_amount_not_negative CHECK (amount_cents >= 0),
                          CONSTRAINT ck_price_original_greater_than_amount CHECK (original_amount_cents IS NULL OR original_amount_cents > amount_cents),
                          CONSTRAINT ck_price_valid_range CHECK (valid_to IS NULL OR valid_to > valid_from)
);

-- Só pode existir UM preço vigente (valid_to nulo) por SKU em cada tenant.
CREATE UNIQUE INDEX uk_price_current
    ON tb_price (tenant_id, sku)
    WHERE valid_to IS NULL;

-- Consulta "qual era o preço do SKU nesta data?"
CREATE INDEX idx_price_sku_period
    ON tb_price (tenant_id, sku, valid_from DESC);