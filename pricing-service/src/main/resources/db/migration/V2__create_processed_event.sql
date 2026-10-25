CREATE TABLE tb_processed_event (
                                    message_id   uuid                         NOT NULL,
                                    processed_at timestamp(6) with time zone  NOT NULL,

                                    CONSTRAINT pk_processed_event PRIMARY KEY (message_id)
);