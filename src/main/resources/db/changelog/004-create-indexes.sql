--liquibase formatted SQL
--changeset charles:004-create-indexes

CREATE INDEX idx_order_item_order_id ON order_item (order_id);
CREATE INDEX idx_order_item_product_id ON order_item (product_id);
CREATE INDEX idx_order_status ON orders (status);
CREATE INDEX idx_order_created_at ON orders (created_at);
CREATE INDEX idx_product_created_at ON product (created_at);
