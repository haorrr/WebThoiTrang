# Spring Boot Backend Analysis Report
**Phase 01: Product Variants Planning**

---

## 1. ENTITY FILES & KEY FIELDS OVERVIEW

### BaseEntity.java
Located: src/main/java/com/fashionshop/entity/BaseEntity.java

Base abstract class for auditable entities:
- `id` (Long): Primary key
- `createdAt` (LocalDateTime): Auto-populated on creation
- `updatedAt` (LocalDateTime): Auto-updated on modification
- `deletedAt` (LocalDateTime): Soft delete flag
- Methods: `isDeleted()`, `softDelete()`

### Product.java
Located: src/main/java/com/fashionshop/entity/Product.java

Extends BaseEntity. Implements soft delete.

**Key Fields:**
- `name` (String, not null): Product name
- `slug` (String, unique, not null): URL identifier
- `description` (TEXT): Full description
- `aiDescription` (TEXT): AI-generated description
- `price` (BigDecimal, not null): Base price
- `salePrice` (BigDecimal, nullable): Sale price
- `stock` (Integer): Available inventory (default: 0)
- `status` (Enum): ACTIVE | INACTIVE
- `category` (ManyToOne, lazy): FK to Category
- `images` (OneToMany): List of ProductImage

**Methods:**
- `getEffectivePrice()`: Returns salePrice if set, else price

### CartItem.java
Located: src/main/java/com/fashionshop/entity/CartItem.java

NOT extending BaseEntity (owns timestamps).

**Key Fields:**
- `id` (Long): PK
- `user` (ManyToOne, lazy): FK to User
- `product` (ManyToOne, eager): FK to Product
- `quantity` (Integer, default: 1): Item count
- `size` (String, length 20): Size variant
- `color` (String, length 50): Color variant
- `createdAt` / `updatedAt` (LocalDateTime): @PrePersist/@PreUpdate

**DB Constraint:** UNIQUE (user_id, product_id, size, color)

### OrderItem.java
Located: src/main/java/com/fashionshop/entity/OrderItem.java

NOT extending BaseEntity.

**Key Fields:**
- `id` (Long): PK
- `order` (ManyToOne, lazy): FK to Order
- `product` (ManyToOne, eager): FK to Product
- `quantity` (Integer): Quantity ordered
- `price` (BigDecimal, not null): Snapshot price at order time
- `size` (String, length 20): Size at order time
- `color` (String, length 50): Color at order time
- `createdAt` / `updatedAt` (LocalDateTime): @PrePersist/@PreUpdate

### Other Entities Summary:

| Entity | Extends | Soft Delete | Purpose |
|--------|---------|-------------|---------|
| User | BaseEntity | Yes | Auth, profile, orders, reviews |
| Order | BaseEntity | Yes | Order header with status, totals, items |
| Category | BaseEntity | Yes | Hierarchical product categories |
| ProductImage | - | No | Product image storage |
| Coupon | BaseEntity | Yes | Discount codes with usage tracking |
| Review | BaseEntity | Yes | Product reviews with approval |
| PasswordResetToken | BaseEntity | No | Password reset tokens |

---

## 2. FLYWAY MIGRATION FILES

Located: src/main/resources/db/migration/

### V1__init.sql (2026-03-26)

**Tables created (in FK dependency order):**

1. users: email (unique), password, name, avatar, role, status, provider, provider_id, refresh_token
2. categories: name, slug (unique), description, image_url, status, parent_id (self-ref)
3. products: name, slug (unique), description, ai_description, price, sale_price, stock, status, category_id
4. product_images: product_id (FK->cascade), image_url, is_primary, sort_order
5. coupons: code (unique), discount_type, discount_value, min_order_amount, max_uses, used_count, expires_at, status
6. orders: user_id (FK->restrict), status, total_amount, shipping_address (JSONB), payment_method, coupon_id, discount_amount, notes
7. order_items: order_id (FK->cascade), product_id (FK->restrict), quantity, price, size, color
8. reviews: user_id (FK->cascade), product_id (FK->cascade), rating (1-5), comment, status; UNIQUE (user_id, product_id)
9. cart_items: user_id (FK->cascade), product_id (FK->cascade), quantity, size, color; UNIQUE (user_id, product_id, size, color)
10. password_reset_tokens: user_id (FK->cascade), token (unique), expires_at, used

**Seed:** Default admin user: admin@fashionshop.com / Admin@123 (Bcrypt hashed)

### V2__fix_shipping_address_type.sql

Changed orders.shipping_address from JSONB to TEXT (Order entity uses plain string)

---

## 3. KEY SERVICE IMPLEMENTATIONS

### CartService.java
Located: src/main/java/com/fashionshop/service/CartService.java

**Methods:**
- `getCart(Long userId)`: Fetch all cart items, calculate subtotal
- `addItem(Long userId, AddCartItemRequest)`: Add product with size/color, validate stock
- `updateItem(Long userId, Long itemId, UpdateCartItemRequest)`: Update quantity
- `removeItem(Long userId, Long itemId)`: Delete cart item
- `clearCart(Long userId)`: Clear entire cart

**Special Note:**
- Line 60: Checks for existing item by (user_id, product_id, size, color)
- Line 64: Uses RuntimeException as "break flow trick" to update existing items

### OrderService.java
Located: src/main/java/com/fashionshop/service/OrderService.java

**createOrder(Long userId, CreateOrderRequest) Flow:**

1. Fetch cart items; throw if empty
2. Fetch user record
3. Validate stock for all items
4. Calculate total: sum(effectivePrice * quantity)
5. Validate coupon if provided; calculate discount
6. Create Order with user, totals, shipping, payment method
7. For each cart item, create OrderItem snapshot (price, size, color)
8. Decrement product stock
9. Increment coupon usage
10. Save order
11. Clear user cart
12. Return OrderResponse

**Other methods:**
- `getOrders()`: Paginated user orders with optional status filter
- `getAllOrders()`: Admin view of all orders
- `getOrderById()`: Fetch single order (permission checks)
- `updateStatus()`: Change status with validation
- `cancelOrder()`: Restore stock, decrement coupon usage
- `validateStatusTransition()`: State machine (PENDING->CONFIRMED|CANCELLED, etc.)

---

## 4. SECURITY CONFIG

Located: src/main/java/com/fashionshop/security/SecurityConfig.java

**Public Paths (no auth required):**
- /api/auth/**
- /oauth2/**, /login/oauth2/**
- /, /index.html, *.html, /admin/*.html, /oauth2/*.html
- /*.css, /*.js, /css/**, /js/**, /images/**, /favicon.ico
- GET /api/products/**, GET /api/categories/**, GET /api/reviews/**
- /swagger-ui/**, /swagger-ui.html, /api-docs/**, /api-docs.yaml
- /actuator/health, /actuator/info

**Protected:** All other /api/** and app endpoints require authentication

**Auth Flow:**
- JWT filter: JwtAuthFilter before UsernamePasswordAuthenticationFilter
- OAuth2: Google, GitHub via CustomOAuth2UserService, CustomOidcUserService
- Session: IF_REQUIRED
- CSRF: Disabled
- Exception: Custom JSON 401 handler for /api/**

---

## 5. CURRENT VARIANT SUPPORT & GAPS

### Current Design (Size/Color):
- CartItem: size (varchar 20), color (varchar 50) columns
- OrderItem: size, color snapshot at order time
- UNIQUE (user_id, product_id, size, color) constraint on cart_items

### Missing for Full Variant System:
1. **ProductVariant entity**: No dedicated variant table (only string columns)
2. **Per-variant inventory**: Stock only at Product level
3. **Per-variant pricing**: No price override per variant
4. **Per-variant images**: No variant-specific media
5. **Flexible attributes**: Only hardcoded size/color; no configurable SKU attributes

### Design Impact:
- Current: Simple denormalized approach
- Needed: ProductVariant table with SKU, attributes, pricing, stock, images

---

## Questions for Phase 01:

1. Separate ProductVariant entity or embedded model?
2. Variant-specific images (separate table)?
3. Backfill migration strategy for existing products?
4. API: Return variants in product GET responses?
5. Admin UI: Bulk variant upload, matrix editor?
6. Variant inventory: Tracked separately or aggregate to product?
7. Can variants override base product price?

