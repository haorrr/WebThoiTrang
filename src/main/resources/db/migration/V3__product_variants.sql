-- V3__product_variants.sql — Product Variants & Wishlist & Flash Sale
-- Phase 01: Product Variants

CREATE TABLE IF NOT EXISTS product_variants (
    id               BIGSERIAL PRIMARY KEY,
    product_id       BIGINT       NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    size             VARCHAR(20),
    color            VARCHAR(50),
    color_code       VARCHAR(10),
    sku              VARCHAR(100) UNIQUE,
    stock            INTEGER      NOT NULL DEFAULT 0 CHECK (stock >= 0),
    price_adjustment DECIMAL(15,2) NOT NULL DEFAULT 0,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMP,
    UNIQUE (product_id, size, color)
);

CREATE INDEX idx_product_variants_product_id ON product_variants(product_id);
CREATE INDEX idx_product_variants_size ON product_variants(size);
CREATE INDEX idx_product_variants_color ON product_variants(color);

-- Add variant_id FK to cart_items (nullable, backward compat)
ALTER TABLE cart_items ADD COLUMN IF NOT EXISTS variant_id BIGINT REFERENCES product_variants(id) ON DELETE SET NULL;
