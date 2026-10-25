-- V1: schema do catalog-service, igual ao que o Hibernate gerava.
-- Mudanças futuras vão em novas migrations (V2, V3...): esta nunca mais se edita.

CREATE TABLE tb_category (
                             id          uuid                         NOT NULL,
                             created_at  timestamp(6) with time zone  NOT NULL,
                             updated_at  timestamp(6) with time zone  NOT NULL,
                             version     bigint                       NOT NULL,
                             tenant_id   uuid                         NOT NULL,
                             depth       integer                      NOT NULL,
                             path        varchar(500)                 NOT NULL,
                             "position"  integer                      NOT NULL,
                             slug        varchar(255)                 NOT NULL,
                             status      varchar(20)                  NOT NULL,
                             title       varchar(255)                 NOT NULL,
                             parent_id   uuid,
                             CONSTRAINT pk_category PRIMARY KEY (id),
                             CONSTRAINT uk_category_tenant_slug UNIQUE (tenant_id, slug),
                             CONSTRAINT uk_category_tenant_parent_title UNIQUE (tenant_id, parent_id, title),
                             CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES tb_category (id),
                             CONSTRAINT ck_category_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE tb_product (
                            id           uuid                         NOT NULL,
                            created_at   timestamp(6) with time zone  NOT NULL,
                            updated_at   timestamp(6) with time zone  NOT NULL,
                            version      bigint                       NOT NULL,
                            tenant_id    uuid                         NOT NULL,
                            brand        varchar(255)                 NOT NULL,
                            description  varchar(255)                 NOT NULL,
                            status       varchar(20)                  NOT NULL,
                            title        varchar(255)                 NOT NULL,
                            category_id  uuid                         NOT NULL,
                            CONSTRAINT pk_product PRIMARY KEY (id),
                            CONSTRAINT uk_product_tenant_title UNIQUE (tenant_id, title),
                            CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES tb_category (id),
                            CONSTRAINT ck_product_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'))
);

CREATE TABLE tb_product_variant (
                                    id             uuid                         NOT NULL,
                                    created_at     timestamp(6) with time zone  NOT NULL,
                                    updated_at     timestamp(6) with time zone  NOT NULL,
                                    version        bigint                       NOT NULL,
                                    tenant_id      uuid                         NOT NULL,
                                    name           varchar(255)                 NOT NULL,
                                    option_values  jsonb,
                                    "position"     integer                      NOT NULL,
                                    sku            varchar(64)                  NOT NULL,
                                    status         varchar(20)                  NOT NULL,
                                    weight_grams   integer,
                                    product_id     uuid                         NOT NULL,
                                    CONSTRAINT pk_product_variant PRIMARY KEY (id),
                                    CONSTRAINT uk_variant_tenant_sku UNIQUE (tenant_id, sku),
                                    CONSTRAINT fk_variant_product FOREIGN KEY (product_id) REFERENCES tb_product (id),
                                    CONSTRAINT ck_variant_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE tb_product_image (
                                  id          uuid                         NOT NULL,
                                  created_at  timestamp(6) with time zone  NOT NULL,
                                  updated_at  timestamp(6) with time zone  NOT NULL,
                                  version     bigint                       NOT NULL,
                                  tenant_id   uuid                         NOT NULL,
                                  alt_text    varchar(200),
                                  "position"  integer                      NOT NULL,
                                  is_primary  boolean                      NOT NULL,
                                  url         varchar(500)                 NOT NULL,
                                  product_id  uuid                         NOT NULL,
                                  variant_id  uuid,
                                  CONSTRAINT pk_product_image PRIMARY KEY (id),
                                  CONSTRAINT fk_image_product FOREIGN KEY (product_id) REFERENCES tb_product (id),
                                  CONSTRAINT fk_image_variant FOREIGN KEY (variant_id) REFERENCES tb_product_variant (id)
);