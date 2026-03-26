-- ============================================================
-- V1__init.sql  — Fashion Shop Database Schema
-- Created: 2026-03-26
-- Table order respects FK dependencies
-- ============================================================

-- 1. USERS
CREATE TABLE IF NOT EXISTS users (
    id                BIGSERIAL PRIMARY KEY,
    email             VARCHAR(255) NOT NULL UNIQUE,
    password          VARCHAR(255),
    name              VARCHAR(255) NOT NULL,
    avatar            VARCHAR(500),
    role              VARCHAR(20)  NOT NULL DEFAULT 'USER',     -- ADMIN | USER
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE | INACTIVE
    provider          VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',    -- LOCAL | GOOGLE | GITHUB
    provider_id       VARCHAR(255),
    refresh_token     VARCHAR(500),
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at        TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);

-- 2. CATEGORIES (self-referencing)
CREATE TABLE IF NOT EXISTS categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    image_url   VARCHAR(500),
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | INACTIVE
    parent_id   BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP
);

CREATE INDEX idx_categories_slug ON categories(slug);
CREATE INDEX idx_categories_parent_id ON categories(parent_id);

-- 3. PRODUCTS
CREATE TABLE IF NOT EXISTS products (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(255)     NOT NULL,
    slug             VARCHAR(255)     NOT NULL UNIQUE,
    description      TEXT,
    ai_description   TEXT,
    price            DECIMAL(15, 2)   NOT NULL,
    sale_price       DECIMAL(15, 2),
    stock            INTEGER          NOT NULL DEFAULT 0,
    status           VARCHAR(20)      NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | INACTIVE
    category_id      BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    created_at       TIMESTAMP        NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP        NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMP
);

CREATE INDEX idx_products_slug ON products(slug);
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_status ON products(status);
CREATE INDEX idx_products_price ON products(price);

-- 4. PRODUCT IMAGES
CREATE TABLE IF NOT EXISTS product_images (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    image_url   VARCHAR(500) NOT NULL,
    is_primary  BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP
);

CREATE INDEX idx_product_images_product_id ON product_images(product_id);

-- 5. COUPONS
CREATE TABLE IF NOT EXISTS coupons (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(50)    NOT NULL UNIQUE,
    discount_type   VARCHAR(20)    NOT NULL,               -- PERCENT | FIXED
    discount_value  DECIMAL(15, 2) NOT NULL,
    min_order_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    max_uses        INTEGER        NOT NULL DEFAULT 1,
    used_count      INTEGER        NOT NULL DEFAULT 0,
    expires_at      TIMESTAMP      NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | INACTIVE
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP
);

CREATE INDEX idx_coupons_code ON coupons(code);
CREATE INDEX idx_coupons_status ON coupons(status);

-- 6. ORDERS
CREATE TABLE IF NOT EXISTS orders (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    status           VARCHAR(30) NOT NULL DEFAULT 'PENDING',  -- PENDING | CONFIRMED | SHIPPING | DELIVERED | CANCELLED
    total_amount     DECIMAL(15, 2) NOT NULL,
    shipping_address JSONB          NOT NULL,
    payment_method   VARCHAR(50)    NOT NULL DEFAULT 'COD',   -- COD | BANK_TRANSFER | MOMO
    coupon_id        BIGINT REFERENCES coupons(id) ON DELETE SET NULL,
    discount_amount  DECIMAL(15, 2) NOT NULL DEFAULT 0,
    notes            TEXT,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMP
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);

-- 7. ORDER ITEMS
CREATE TABLE IF NOT EXISTS order_items (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT         NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id  BIGINT         NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantity    INTEGER        NOT NULL CHECK (quantity > 0),
    price       DECIMAL(15, 2) NOT NULL,  -- snapshot price at order time
    size        VARCHAR(20),
    color       VARCHAR(50),
    created_at  TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP      NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

-- 8. REVIEWS
CREATE TABLE IF NOT EXISTS reviews (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id  BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    rating      INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING | APPROVED | REJECTED
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP,
    UNIQUE (user_id, product_id)
);

CREATE INDEX idx_reviews_product_id ON reviews(product_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_status ON reviews(status);

-- 9. CART ITEMS
CREATE TABLE IF NOT EXISTS cart_items (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id  BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    quantity    INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
    size        VARCHAR(20),
    color       VARCHAR(50),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP,
    UNIQUE (user_id, product_id, size, color)
);

CREATE INDEX idx_cart_items_user_id ON cart_items(user_id);

-- 10. PASSWORD RESET TOKENS
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP    NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP
);

CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);

-- ============================================================
-- Seed: Default Admin User
-- Password: Admin@123 (BCrypt hashed)
-- ============================================================
INSERT INTO users (email, password, name, role, status, provider)
VALUES (
    'admin@fashionshop.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Admin',
    'ADMIN',
    'ACTIVE',
    'LOCAL'
) ON CONFLICT (email) DO NOTHING;
