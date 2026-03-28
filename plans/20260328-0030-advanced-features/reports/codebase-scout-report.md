# Codebase Scout Report

**Date:** 2026-03-28
**Scanned:** All 102 Java source files + frontend + DB migrations

---

## Existing Structure Inventory

### Backend (src/main/java/com/fashionshop/)

**Entities (10):** BaseEntity, User, Category, Product, ProductImage, CartItem, Coupon, Order, OrderItem, Review, PasswordResetToken

**Repositories (12):** CartItemRepository, CategoryRepository, CouponRepository, OrderItemRepository, OrderRepository, PasswordResetTokenRepository, ProductImageRepository, ProductRepository, ProductSpecification, ReviewRepository, UserRepository, UserSpecification

**Services (13):** AuthService, CartService, CategoryService, CloudinaryService, CouponService, DashboardService, EmailService, GeminiService, JwtService, OrderService, ProductService, ReviewService, UserService

**Controllers (11):** AiController, AuthController, CartController, CategoryController, CouponController, DashboardController, OrderController, ProductController, ReviewController, UploadController, UserController

**DTOs — Request (18):** AddCartItemRequest, CategoryRequest, ChatRequest, CouponRequest, CreateOrderRequest, CreateReviewRequest, ForgotPasswordRequest, LoginRequest, ProductRequest, RefreshTokenRequest, RegisterRequest, ResetPasswordRequest, UpdateCartItemRequest, UpdateOrderStatusRequest, UpdateProfileRequest, UpdateReviewRequest, UpdateUserRequest, ValidateCouponRequest

**DTOs — Response (17):** AuthResponse, CartItemResponse, CartResponse, CategoryResponse, CategoryTreeResponse, ChatResponse, CouponResponse, CouponValidationResponse, DashboardResponse, OrderItemResponse, OrderResponse, ProductImageResponse, ProductResponse, ProductSummaryResponse, ReviewResponse, UserResponse, UserSummaryResponse

**Config (6):** AppConfig, CorsConfig, GeminiConfig, OAuth2Config, OpenApiConfig, RedisConfig

**Security (3 + 5 oauth2):** JwtAuthFilter, CustomUserDetailsService, SecurityConfig + 4 oauth2 handlers

**Exception (4):** BadRequestException, GlobalExceptionHandler, ResourceNotFoundException, UnauthorizedException

**Util (1):** SlugUtil

---

## Existing DB Schema

| Table | Key columns | Indexes |
|-------|------------|---------|
| users | email, role, status, provider, refresh_token | email, status |
| categories | slug, parent_id, status | slug, parent_id |
| products | slug, price, sale_price, stock, status, category_id, ai_description | slug, category_id, status, price |
| product_images | product_id, is_primary, sort_order | product_id |
| coupons | code, discount_type, discount_value, max_uses, used_count, expires_at | code, status |
| orders | user_id, status, total_amount, shipping_address (JSONB), payment_method, coupon_id | user_id, status, created_at |
| order_items | order_id, product_id, price (snapshot), size, color | order_id, product_id |
| reviews | user_id, product_id (UNIQUE pair), rating, status | product_id, user_id, status |
| cart_items | user_id, product_id (UNIQUE+size+color), size, color | user_id |
| password_reset_tokens | user_id, token, expires_at, used | token, user_id |

---

## Key Patterns Identified

1. **Soft delete:** All main entities extend `BaseEntity` which has `deletedAt`. `@Where(clause = "deleted_at IS NULL")` on entities. `softDelete()` method on BaseEntity.

2. **Response DTOs:** All have `static from(Entity e)` factory method — no MapStruct mapper used despite pom.xml dependency.

3. **Cache:** `@Cacheable` / `@CacheEvict` on ProductService and CategoryService. Redis TTL configured globally at 10 min. No per-cache TTL override currently.

4. **Auth extraction:** Controllers get userId via `Authentication authentication` param + `UserDetailsService`.

5. **Validation:** `@Valid` on request bodies, `@NotNull`, `@NotBlank`, `@Min` annotations on DTOs.

6. **Error handling:** `GlobalExceptionHandler` catches `BadRequestException`, `ResourceNotFoundException`, `UnauthorizedException` → `ApiResponse` format.

7. **Async:** `EmailService` methods annotated `@Async`. `@EnableAsync` in `AppConfig`.

8. **Scheduling:** NOT yet enabled — `@EnableScheduling` needed for Phase 03+.

9. **Paging:** `Page<T>` with whitelist sort field validation pattern in Service classes.

10. **Specification:** `ProductSpecification` and `UserSpecification` for dynamic filtering.

---

## Existing Frontend Files

**Static HTML pages (user):** index, products, product-detail, cart, checkout-success, orders, order-detail, profile, login, register, forgot-password, reset-password

**Static HTML pages (admin):** dashboard, products, categories, orders, users, coupons, reviews

**JS:** api.js (API client), auth.js (JWT handling), utils.js (shared helpers), script.js (global nav/cart badge)

**CSS:** styles.css (main), shared.css (components)

---

## Missing Dependencies (for new phases)

| Dependency | Phase | Needed For |
|-----------|-------|------------|
| `@EnableScheduling` | Phase 03 | Flash sale activation/deactivation |
| Apache POI `poi-ooxml:5.2.5` | Phase 11 | Excel export |
| No new external services required | — | All other phases use existing stack |

---

## Constraints & Gotchas

1. `ddl-auto: validate` — Hibernate validates schema against DB. All new tables MUST be in Flyway migrations before JPA entities are added, otherwise startup fails.

2. `cart_items` unique constraint is `(user_id, product_id, size, color)` — Phase 01 adds `variant_id` column; existing constraint remains valid since size/color still stored as strings.

3. `@Where(clause = "deleted_at IS NULL")` on entities means soft-deleted records are invisible to all JPA queries — admin views that need to see deleted records require native queries or `@Filter` override.

4. `MapStruct` is in pom.xml but NOT used — all mapping is manual `static from()`. Do not introduce MapStruct in new phases to maintain consistency.

5. `GeminiService` uses Java 11 `HttpClient` (not Spring `WebClient`/`RestTemplate`) — maintain this pattern for AI calls.

6. `Coupon` entity has `isValid()` and `calculateDiscount()` business methods directly on entity — maintain this pattern for domain logic.
