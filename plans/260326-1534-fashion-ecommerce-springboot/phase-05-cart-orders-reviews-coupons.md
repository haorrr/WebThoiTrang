# Phase 05 — Cart, Orders, Reviews & Coupons

## Context Links

- [plan.md](plan.md)
- [Research 01 — Architecture](research/researcher-01-report.md)
- Depends on: Phase 02 (auth/user), Phase 04 (Product entity + stock)

---

## Overview

| Field | Value |
|-------|-------|
| Date | 2026-03-26 |
| Priority | HIGH |
| Implementation Status | PENDING |
| Review Status | PENDING |
| Description | Shopping cart (per-user), order creation from cart, order status flow, product reviews with moderation, and coupon system with validation. |

---

## Key Insights

- Cart is ephemeral — no order history needed from cart. On `POST /api/orders`, cart items are read, an order is created, then cart is cleared. Atomic operation via `@Transactional`.
- Order `shippingAddress` stored as JSON string (PostgreSQL `TEXT` column) — avoids separate address table for academic scope.
- `OrderItem.price` is a snapshot of product price at order time — product price changes must not affect historical orders.
- Coupon validation is separate from order creation: frontend calls `POST /api/coupons/validate` first, then submits order with `couponCode`; backend re-validates on order create.
- Review moderation: created with `status=PENDING`, only APPROVED reviews shown publicly. ADMIN can bulk-approve via `PATCH /api/reviews/{id}/status`.

---

## Requirements

### Functional
**Cart:**
- `GET /api/cart` — user's cart items with product snapshots
- `POST /api/cart/items` — add/increment item
- `PUT /api/cart/items/{id}` — update quantity
- `DELETE /api/cart/items/{id}` — remove one item
- `DELETE /api/cart` — clear all

**Orders:**
- `POST /api/orders` — create from cart, apply coupon, decrement stock
- `GET /api/orders` — ADMIN: all; USER: own orders (paginated)
- `GET /api/orders/{id}` — detail with items
- `PATCH /api/orders/{id}/status` — ADMIN status flow
- `POST /api/orders/{id}/cancel` — USER cancel own PENDING order, restore stock

**Reviews:**
- `POST /api/reviews` — authenticated user, once per product (enforce)
- `GET /api/reviews?productId=&userId=&status=` — public (APPROVED only for public)
- `PUT /api/reviews/{id}` — own review
- `DELETE /api/reviews/{id}` — own or ADMIN
- `PATCH /api/reviews/{id}/status` — ADMIN moderate

**Coupons (ADMIN):**
- `GET/POST/PUT/DELETE /api/coupons`
- `PATCH /api/coupons/{id}/status`
- `POST /api/coupons/validate` — USER, returns discount amount preview

### Technical
- Order status machine: `PENDING → CONFIRMED → SHIPPING → DELIVERED` (forward only; CANCELLED is terminal)
- Stock check before order create; decrement atomically
- Coupon: check expiry, `usedCount < maxUses`, `minOrderAmount`, then increment `usedCount` on order create
- One review per user per product (unique constraint in DB + service check)

---

## Architecture

### Package Structure
```
com.fashionshop/
├── entity/
│   ├── CartItem.java
│   ├── Order.java
│   ├── OrderItem.java
│   ├── Review.java
│   └── Coupon.java
├── repository/
│   ├── CartItemRepository.java
│   ├── OrderRepository.java
│   ├── OrderItemRepository.java
│   ├── ReviewRepository.java
│   └── CouponRepository.java
├── dto/
│   ├── request/
│   │   ├── AddCartItemRequest.java
│   │   ├── UpdateCartItemRequest.java
│   │   ├── CreateOrderRequest.java
│   │   ├── UpdateOrderStatusRequest.java
│   │   ├── CreateReviewRequest.java
│   │   ├── UpdateReviewRequest.java
│   │   ├── CouponRequest.java
│   │   └── ValidateCouponRequest.java
│   └── response/
│       ├── CartResponse.java
│       ├── CartItemResponse.java
│       ├── OrderResponse.java
│       ├── OrderItemResponse.java
│       ├── ReviewResponse.java
│       ├── CouponResponse.java
│       └── CouponValidationResponse.java
├── service/
│   ├── CartService.java
│   ├── OrderService.java
│   ├── ReviewService.java
│   └── CouponService.java
└── controller/
    ├── CartController.java
    ├── OrderController.java
    ├── ReviewController.java
    └── CouponController.java
```

### Order Create Flow
```
1. Load user's cart items (fail if empty)
2. Validate coupon (if provided): expiry, minAmount, uses
3. Calculate totalAmount (sum of item.price * qty)
4. Apply discount from coupon
5. Check stock for each product
6. Create Order + OrderItems (price snapshot = product.salePrice ?? product.price)
7. Decrement stock for each product
8. Increment coupon.usedCount (if applied)
9. Clear cart
All steps in single @Transactional method
```

---

## Related Code Files

### Create
- `src/main/java/com/fashionshop/entity/CartItem.java`
- `src/main/java/com/fashionshop/entity/Order.java`
- `src/main/java/com/fashionshop/entity/OrderItem.java`
- `src/main/java/com/fashionshop/entity/Review.java`
- `src/main/java/com/fashionshop/entity/Coupon.java`
- `src/main/java/com/fashionshop/repository/CartItemRepository.java`
- `src/main/java/com/fashionshop/repository/OrderRepository.java`
- `src/main/java/com/fashionshop/repository/OrderItemRepository.java`
- `src/main/java/com/fashionshop/repository/ReviewRepository.java`
- `src/main/java/com/fashionshop/repository/CouponRepository.java`
- `src/main/java/com/fashionshop/dto/request/AddCartItemRequest.java`
- `src/main/java/com/fashionshop/dto/request/UpdateCartItemRequest.java`
- `src/main/java/com/fashionshop/dto/request/CreateOrderRequest.java`
- `src/main/java/com/fashionshop/dto/request/UpdateOrderStatusRequest.java`
- `src/main/java/com/fashionshop/dto/request/CreateReviewRequest.java`
- `src/main/java/com/fashionshop/dto/request/UpdateReviewRequest.java`
- `src/main/java/com/fashionshop/dto/request/CouponRequest.java`
- `src/main/java/com/fashionshop/dto/request/ValidateCouponRequest.java`
- `src/main/java/com/fashionshop/dto/response/CartResponse.java`
- `src/main/java/com/fashionshop/dto/response/CartItemResponse.java`
- `src/main/java/com/fashionshop/dto/response/OrderResponse.java`
- `src/main/java/com/fashionshop/dto/response/OrderItemResponse.java`
- `src/main/java/com/fashionshop/dto/response/ReviewResponse.java`
- `src/main/java/com/fashionshop/dto/response/CouponResponse.java`
- `src/main/java/com/fashionshop/dto/response/CouponValidationResponse.java`
- `src/main/java/com/fashionshop/service/CartService.java`
- `src/main/java/com/fashionshop/service/OrderService.java`
- `src/main/java/com/fashionshop/service/ReviewService.java`
- `src/main/java/com/fashionshop/service/CouponService.java`
- `src/main/java/com/fashionshop/controller/CartController.java`
- `src/main/java/com/fashionshop/controller/OrderController.java`
- `src/main/java/com/fashionshop/controller/ReviewController.java`
- `src/main/java/com/fashionshop/controller/CouponController.java`
- `src/main/resources/db/migration/V3__add_unique_review.sql`

---

## Implementation Steps

1. **Add V3 migration** for unique constraint on reviews:
   ```sql
   ALTER TABLE reviews ADD CONSTRAINT uq_user_product_review UNIQUE (user_id, product_id);
   ```

2. **Create entities**:

   `CartItem` (no BaseEntity — just `createdAt`):
   - Fields: `id`, `user` (ManyToOne), `product` (ManyToOne), `quantity`, `size`, `color`, `createdAt`

   `Order` extends `BaseEntity`:
   - Fields: `user` (ManyToOne), `status` (enum), `totalAmount` (BigDecimal), `shippingAddress` (TEXT/String), `paymentMethod`, `coupon` (ManyToOne nullable), `discountAmount`
   - `@OneToMany(mappedBy = "order", cascade = ALL)` to `OrderItem`
   - `@Where(clause = "deleted_at IS NULL")`

   `OrderItem` (no BaseEntity):
   - Fields: `id`, `order` (ManyToOne), `product` (ManyToOne), `quantity`, `price` (BigDecimal snapshot), `size`, `color`

   `Review` extends `BaseEntity`:
   - Fields: `user` (ManyToOne), `product` (ManyToOne), `rating` (int 1-5), `comment`, `status` (PENDING/APPROVED/REJECTED)
   - `@Where(clause = "deleted_at IS NULL")`

   `Coupon` extends `BaseEntity`:
   - Fields: `code` (unique), `discountType` (PERCENT/FIXED), `discountValue` (BigDecimal), `minOrderAmount`, `maxUses`, `usedCount`, `expiresAt`, `status`
   - `@Where(clause = "deleted_at IS NULL")`

3. **Create repositories**:
   - `CartItemRepository`: `findByUserId`, `findByUserIdAndProductIdAndSizeAndColor`, `deleteByUserId`
   - `OrderRepository`: `findByUserId(Pageable)`, `findByIdAndUserId`
   - `ReviewRepository`: `findByProductIdAndStatus`, `existsByUserIdAndProductId`
   - `CouponRepository`: `findByCode`, `findByCodeAndStatusAndExpiresAtAfter`

4. **Implement `CartService`**:
   - `getCart(Long userId)` → fetch items → map to `CartResponse` (include product snapshot: name, price, image)
   - `addItem(Long userId, AddCartItemRequest)` → if same product+size+color exists, increment qty; else create new
   - `updateItem(Long userId, Long itemId, int quantity)` → find item, verify ownership, update
   - `removeItem(Long userId, Long itemId)` → verify ownership → delete
   - `clearCart(Long userId)` → `deleteByUserId`

5. **Implement `CouponService`**:
   - `validateCoupon(String code, BigDecimal orderAmount)` → check code, status=ACTIVE, not expired, usedCount<maxUses, orderAmount>=minOrderAmount → calculate discount → return `CouponValidationResponse`
   - `calculateDiscount(Coupon c, BigDecimal amount)` → PERCENT: `amount * value / 100`; FIXED: `min(value, amount)`
   - CRUD methods for ADMIN

6. **Implement `OrderService`**:
   - `createOrder(Long userId, CreateOrderRequest req)` — full `@Transactional` flow (see Architecture section)
   - `getOrders(Long userId, String role, Pageable)` — ADMIN gets all; USER gets own
   - `getOrderById(Long orderId, Long userId, String role)` — ownership check for USER
   - `updateStatus(Long orderId, String newStatus)` — validate state machine transitions
   - `cancelOrder(Long orderId, Long userId)` — check PENDING, set CANCELLED, restore stock

   Valid status transitions:
   ```
   PENDING     → CONFIRMED, CANCELLED
   CONFIRMED   → SHIPPING, CANCELLED
   SHIPPING    → DELIVERED
   DELIVERED   → (terminal)
   CANCELLED   → (terminal)
   ```

7. **Implement `ReviewService`**:
   - `createReview(Long userId, CreateReviewRequest)` → check user purchased product (optional for academic) → check no existing review → save with PENDING status
   - `getReviews(Long productId, Long userId, String status)` — public: APPROVED only; ADMIN: all statuses
   - `updateReview(Long userId, Long reviewId, UpdateReviewRequest)` → ownership check → update
   - `deleteReview(Long userId, Long reviewId, String role)` → ownership or ADMIN
   - `moderateReview(Long reviewId, String status)` — ADMIN only

8. **Create controllers** — all return `ResponseEntity<ApiResponse<T>>`:
   - `CartController` — all endpoints `@PreAuthorize("isAuthenticated()")`
   - `OrderController` — create/cancel: USER; updateStatus: ADMIN
   - `ReviewController` — getReviews: public; write: authenticated; moderate: ADMIN
   - `CouponController` — CRUD: ADMIN; validate: authenticated

9. **Test end-to-end order flow**:
   - Add items to cart → `POST /api/orders` → verify order created, stock decremented, cart cleared

---

## Todo List

- [ ] Write `V3__add_unique_review.sql`
- [ ] Create `CartItem`, `Order`, `OrderItem`, `Review`, `Coupon` entities
- [ ] Create all 5 repositories
- [ ] Create all request DTOs (8)
- [ ] Create all response DTOs (7)
- [ ] Implement `CartService` (5 methods)
- [ ] Implement `CouponService` (validate + CRUD)
- [ ] Implement `OrderService` (5 methods, @Transactional create)
- [ ] Implement `ReviewService` (5 methods)
- [ ] Create `CartController`
- [ ] Create `OrderController`
- [ ] Create `ReviewController`
- [ ] Create `CouponController`
- [ ] Test full order flow (cart → order → stock decremented)
- [ ] Test coupon validation + discount applied to order
- [ ] Test order cancellation + stock restored
- [ ] Test review moderation flow

---

## Success Criteria

- `POST /api/orders` with cart items → 201 with order; stock decremented
- `POST /api/orders` with empty cart → 400 "Cart is empty"
- `POST /api/orders` with out-of-stock product → 400 "Insufficient stock"
- `POST /api/coupons/validate` with valid code → 200 with discount amount
- `POST /api/reviews` twice for same product → 409 Conflict
- `PATCH /api/orders/{id}/status` DELIVERED→PENDING → 400 "Invalid status transition"
- `GET /api/reviews?productId=1` (public) → only APPROVED reviews

---

## Risk Assessment

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Race condition on stock decrement | Medium | Use optimistic locking `@Version` on Product.stock OR pessimistic lock with `SELECT FOR UPDATE` in OrderService |
| Order total doesn't match coupon recomputation | Medium | Always re-validate coupon in `createOrder()` regardless of frontend preview |
| `shippingAddress` JSON parsing issues | Low | Store as plain String; deserialize to `AddressDTO` only when needed |
| Cart items referencing deleted products | Low | `CartItem.product` ManyToOne without `@Where` — handle in service when product is null/deleted |

---

## Security Considerations

- Users can only see/modify their own cart and orders — enforce `userId` checks in service layer
- `PATCH /api/orders/{id}/status` is ADMIN-only — prevent users from self-confirming orders
- Coupon `usedCount` incremented atomically inside `@Transactional` — prevents abuse
- Review moderation prevents spam/fake reviews from being public before ADMIN approval

---

## Next Steps

- Phase 6 (AI) may use `OrderItem` + `Product` data for recommendations
- Phase 7 (Redis) can cache `getReviews` per product (invalidate on new review)
