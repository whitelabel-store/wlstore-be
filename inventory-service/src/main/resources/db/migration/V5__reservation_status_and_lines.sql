-- V5: para liberar ou confirmar um pedido, o inventory precisa saber o que ele reservou e em que estado está.

ALTER TABLE tb_reservation
    ADD COLUMN status varchar(16) NOT NULL DEFAULT 'RESERVED';

ALTER TABLE tb_reservation
    ADD CONSTRAINT ck_reservation_status CHECK (status IN ('RESERVED', 'RELEASED', 'CONFIRMED'));

CREATE TABLE tb_reservation_line (
                                     id              uuid                         NOT NULL,
                                     created_at      timestamp(6) with time zone  NOT NULL,
                                     updated_at      timestamp(6) with time zone  NOT NULL,
                                     version         bigint                       NOT NULL,
                                     tenant_id       uuid                         NOT NULL,
                                     reservation_id  uuid                         NOT NULL,
                                     sku             varchar(64)                  NOT NULL,
                                     quantity        integer                      NOT NULL,
                                     CONSTRAINT pk_reservation_line PRIMARY KEY (id),
                                     CONSTRAINT fk_reservation_line_reservation FOREIGN KEY (reservation_id) REFERENCES tb_reservation (id),
                                     CONSTRAINT ck_reservation_line_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX ix_reservation_line_reservation ON tb_reservation_line (reservation_id);