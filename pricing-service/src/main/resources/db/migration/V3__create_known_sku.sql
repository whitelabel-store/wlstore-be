CREATE TABLE tb_known_sku (
                              id          uuid         NOT NULL,
                              created_at  timestamptz(6) NOT NULL,
                              updated_at  timestamptz(6) NOT NULL,
                              version     bigint       NOT NULL,
                              tenant_id   uuid         NOT NULL,
                              product_id  uuid         NOT NULL,
                              sku         varchar(64)  NOT NULL,
                              CONSTRAINT pk_known_sku PRIMARY KEY (id),
                              CONSTRAINT uk_known_sku_tenant_sku UNIQUE (tenant_id, sku)
);

CREATE INDEX idx_known_sku_product ON tb_known_sku (tenant_id, product_id);