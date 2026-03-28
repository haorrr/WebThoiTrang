# Backend Exploration Report Index
**Generated:** 2026-03-28

## Quick Navigation

### Start Here
→ **EXPLORATION_SUMMARY.md** - Executive summary of backend architecture and findings

### Detailed Reference
→ **backend-analysis.md** - Complete entity definitions, service implementations, security config
→ **file-locations.md** - Quick lookup of all relevant file paths

---

## What You'll Find

### Section 1: Entity Architecture (backend-analysis.md)
- **BaseEntity**: Auditable base class with soft delete
- **Product**: Core product entity with stock/price
- **CartItem/OrderItem**: Variant support with size/color
- **Order/User/Category**: Order management & auth
- **Supporting entities**: Reviews, Coupons, Images, Tokens

### Section 2: Database Schema (backend-analysis.md)
- **V1__init.sql**: 10 tables with proper FKs, indexes, seed data
- **V2__fix_shipping_address_type.sql**: JSONB -> TEXT conversion
- Design patterns: soft delete, unique constraints, cascades

### Section 3: Service Layer (backend-analysis.md)
- **CartService**: Stock validation, size/color deduplication
- **OrderService**: 12-step createOrder flow, state machine validation

### Section 4: Security Config (backend-analysis.md)
- Public paths: /api/products, /api/auth, /oauth2
- Protected paths: /api/cart, /api/orders, /api/admin
- Auth: JWT filter + OAuth2 (Google/GitHub)

### Section 5: Variant Gap Analysis (EXPLORATION_SUMMARY.md)
- Current: size/color strings only
- Missing: ProductVariant entity, per-variant inventory, per-variant pricing
- Recommendations: Full variant system design

---

## Key Files by Category

**Entity Files (11 total)**
- Product, CartItem, OrderItem, Order
- User, Category, ProductImage, Coupon, Review
- PasswordResetToken, BaseEntity

**Service Files (2 critical)**
- CartService: cart management with stock validation
- OrderService: order creation with coupon handling

**Configuration**
- SecurityConfig.java: JWT + OAuth2 setup

**Database Migrations**
- V1__init.sql: Schema definition
- V2__fix_shipping_address_type.sql: Migration fix

---

## Phase 01 Context

This backend exploration is for planning **Phase 01: Product Variants** implementation.

Current state: Basic size/color support in CartItem/OrderItem strings
Needed state: Full ProductVariant system with SKU, per-variant inventory, pricing, images

See EXPLORATION_SUMMARY.md section "Phase 01 Recommendations" for next steps.

