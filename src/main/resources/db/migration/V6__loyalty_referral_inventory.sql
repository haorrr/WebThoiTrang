-- V6__loyalty_referral_inventory.sql — Loyalty Points, Referral System, Inventory

-- Phase 08: Loyalty Points
CREATE TABLE IF NOT EXISTS loyalty_points (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    order_id    BIGINT REFERENCES orders(id) ON DELETE SET NULL,
    points      INTEGER NOT NULL,           -- positive = earned, negative = redeemed
    type        VARCHAR(20) NOT NULL,       -- EARNED | REDEEMED | EXPIRED | REFERRAL_BONUS
    description VARCHAR(255),
    expires_at  TIMESTAMP,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_loyalty_user_id ON loyalty_points(user_id);
CREATE INDEX idx_loyalty_type ON loyalty_points(type);

-- Phase 09: Referral System
ALTER TABLE users ADD COLUMN IF NOT EXISTS referral_code VARCHAR(20) UNIQUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS referred_by   BIGINT REFERENCES users(id) ON DELETE SET NULL;

-- Phase 10: Inventory Management — stock movement history
CREATE TABLE IF NOT EXISTS stock_movements (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    variant_id  BIGINT REFERENCES product_variants(id) ON DELETE SET NULL,
    quantity    INTEGER NOT NULL,           -- positive = in, negative = out
    type        VARCHAR(30) NOT NULL,       -- ORDER | CANCEL | MANUAL_ADJUST | RESTOCK
    reference   VARCHAR(100),              -- order_id or admin note
    created_by  BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stock_movements_product_id ON stock_movements(product_id);
CREATE INDEX idx_stock_movements_type ON stock_movements(type);

-- Phase 06: Order tracking — add tracking number and estimated delivery
ALTER TABLE orders ADD COLUMN IF NOT EXISTS tracking_number  VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS estimated_delivery DATE;
