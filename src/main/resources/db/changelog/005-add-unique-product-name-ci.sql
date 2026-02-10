--liquibase formatted sql
--changeset charles:005-add-unique-product-name-ci

CREATE UNIQUE INDEX uk_product_name_ci ON product (LOWER(BTRIM(name)));
