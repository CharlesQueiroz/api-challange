--liquibase formatted SQL
--changeset charles:001-create-product-table

CREATE TABLE product
(
    id             BIGSERIAL PRIMARY KEY,
    code           UUID           NOT NULL DEFAULT gen_random_uuid(),
    name           VARCHAR(255)   NOT NULL,
    description    VARCHAR(2000),
    price          NUMERIC(19, 2) NOT NULL,
    stock_quantity INTEGER        NOT NULL DEFAULT 0,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version        BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT uk_product_code UNIQUE (code),
    CONSTRAINT ck_product_price_positive CHECK (price > 0),
    CONSTRAINT ck_product_stock_non_negative CHECK (stock_quantity >= 0)
);
