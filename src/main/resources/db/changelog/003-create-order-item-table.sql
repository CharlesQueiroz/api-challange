--liquibase formatted SQL
--changeset charles:003-create-order-item-table

CREATE TABLE order_item
(
    id           BIGSERIAL PRIMARY KEY,
    code         UUID           NOT NULL DEFAULT gen_random_uuid(),
    order_id     BIGINT         NOT NULL,
    product_id   BIGINT,
    product_name VARCHAR(255)   NOT NULL,
    unit_price   NUMERIC(19, 2) NOT NULL,
    quantity     INTEGER        NOT NULL,
    created_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version      BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT uk_order_item_code UNIQUE (code),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE SET NULL,
    CONSTRAINT ck_order_item_unit_price_positive CHECK (unit_price > 0),
    CONSTRAINT ck_order_item_quantity_positive CHECK (quantity > 0)
);
