--liquibase formatted SQL
--changeset charles:002-create-order-table

CREATE TABLE orders
(
    id             BIGSERIAL PRIMARY KEY,
    code           UUID           NOT NULL DEFAULT gen_random_uuid(),
    customer_name  VARCHAR(255)   NOT NULL,
    customer_email VARCHAR(255)   NOT NULL,
    status         VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    total_amount   NUMERIC(19, 2) NOT NULL DEFAULT 0,
    order_date     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version        BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT uk_order_code UNIQUE (code),
    CONSTRAINT ck_order_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_order_total_non_negative CHECK (total_amount >= 0)
);
