# Spring Boot Backend Exploration - Complete Summary
**Date:** 2026-03-28  
**Purpose:** Phase 01 Implementation Planning (Product Variants)

---

## Key Findings

### 1. ENTITY ARCHITECTURE

**11 Total Entities** with clear relationships:

**Core Domain:**
- `Product`: Base entity with stock, price, sale_price. Extends BaseEntity (soft delete support)
- `CartItem`: Links User -> Product with size/color variant fields
- `OrderItem`: Snapshots product data (price, size, color) at order time
- `Order`: Order header with status state machine, totals, items

**Support Entities:**
- `User`: Auth-ready with OAuth2 provider fields
- `Category`: Hierarchical structure (self-referencing parent)
- `ProductImage`: Product media with primary flag and sort order
- `Coupon`: Discount codes with usage tracking
- `Review`: Product reviews with approval workflow
- `PasswordResetToken`: Token-based password reset

**Framework:**
- `BaseEntity`: Abstract parent providing ID, createdAt, updatedAt, deletedAt (soft delete pattern)

### 2. DATABASE SCHEMA

**Flyway Migrations (2 files):**
- `V1__init.sql`: Initial schema with 10 tables, proper FK relationships (cascade vs restrict), indexes, default admin seed
- `V2__fix_shipping_address_type.sql`: JSONB -> TEXT conversion for shipping_address

**Critical Design Details:**
- shipping_address stored as plain TEXT (not JSONB JSON)
- cart_items has UNIQUE (user_id, product_id, size, color) constraint
- order_items snapshots size/color for historical accuracy
- Soft delete implemented via deletedAt IS NULL WHERE clause

### 3. SERVICE LAYER INSIGHTS

**CartService:**
- Adds items with size/color deduplication
- Line 64 uses RuntimeException "hack" to break flow on duplicate updates
- Validates stock before adding items
- Calculates subtotal with effective price (sale_price | base price)

**OrderService:**
- createOrder() has 12-step flow (validate -> calculate -> decrement stock -> save)
- Snapshots pricing and variant info in OrderItem
- Validates coupon eligibility before applying discount
- State machine for order status transitions (PENDING -> CONFIRMED -> SHIPPING -> DELIVERED)
- cancelOrder() restores stock and coupon usage

### 4. SECURITY MODEL

**Public API Paths:**
- /api/auth/**, /api/products/**, /api/categories/**, /api/reviews/** (GET only)
- /swagger-ui/**, /oauth2/**, Actuator health/info

**Protected Paths:**
- /api/cart/**, /api/orders/**, /api/users/**, /api/admin/**

**Auth Flow:**
- JWT filter (JwtAuthFilter) before form auth
- OAuth2 enabled (Google, GitHub)
- Session: IF_REQUIRED
- CSRF disabled

---

## Current Variant Support

### What Exists:
- CartItem.size (varchar 20)
- CartItem.color (varchar 50)
- OrderItem.size, OrderItem.color (snapshots)
- UNIQUE constraint prevents duplicate variants in cart

### What's Missing:
1. ProductVariant entity (no dedicated SKU/variant table)
2. Per-variant inventory tracking (only Product.stock)
3. Per-variant pricing (no override mechanism)
4. Per-variant images (only Product.images)
5. Flexible attribute system (only hardcoded size/color)

---

## Phase 01 Recommendations

### Minimal Variant Support (Without Major Refactor):
- Keep size/color as strings in CartItem/OrderItem
- Enhance CartService stock validation: product.getStock(size, color) instead of product.getStock()
- Migrate existing products: Create default variant entries

### Full Variant System (Recommended):
- New `ProductVariant` entity with:
  - `sku` (unique identifier)
  - `size`, `color`, and other attributes (flexible)
  - `stock` (per-variant inventory)
  - `price` (optional override)
  - `images` (variant-specific media)
- Refactor CartItem/OrderItem to reference ProductVariant
- Create migration: Backfill existing products with default variants
- Update OrderService: Calculate totals from variant prices

---

## Report Files Generated

1. **backend-analysis.md** (207 lines)
   - Complete entity field documentation
   - Service method implementations
   - Security configuration details
   - Design gap analysis
   
2. **file-locations.md**
   - All file absolute paths for quick reference

3. **EXPLORATION_SUMMARY.md** (this file)
   - Executive summary of findings

---

## Files Ready for Reference

**Entity Files:**
- BaseEntity.java, User.java, Product.java
- CartItem.java, OrderItem.java, Order.java
- Category.java, ProductImage.java, Coupon.java
- Review.java, PasswordResetToken.java

**Service Files:**
- CartService.java (with stock validation)
- OrderService.java (with createOrder flow)

**Config Files:**
- SecurityConfig.java (auth paths, OAuth2 setup)

**Migrations:**
- V1__init.sql (10 tables, 10 indexes, default admin)
- V2__fix_shipping_address_type.sql (JSONB -> TEXT fix)

---

## Next Steps for Phase 01

1. Review ProductVariant design approach (separate entity vs embedded)
2. Plan variant attribute schema (size, color, others?)
3. Design SKU generation strategy
4. Plan inventory tracking: product-level or variant-level?
5. Create migration strategy for backfilling existing products
6. Update CartService stock validation logic
7. Update OrderService pricing calculation

