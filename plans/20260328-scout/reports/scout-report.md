# Scout Report: WebThoiTrang Codebase Analysis
Date: 2026-03-28 | All requested components found

## 1. REVIEW ENTITY/SERVICE/CONTROLLER/REPOSITORY

Entity: /d/Desktop/WebThoiTrang/src/main/java/com/fashionshop/entity/Review.java
- Fields: user (ManyToOne), product (ManyToOne), rating (Integer), comment (TEXT)
- Status enum: PENDING, APPROVED, REJECTED (default PENDING)

Service: /d/Desktop/WebThoiTrang/src/main/java/com/fashionshop/service/ReviewService.java
- getProductReviews(Long productId, boolean approvedOnly, int page, int size)
- createReview(Long userId, CreateReviewRequest req) - sets PENDING status
- updateReview(Long userId, Long reviewId, UpdateReviewRequest req) - resets to PENDING
- deleteReview(Long userId, Long reviewId, boolean isAdmin) - soft delete
- moderateReview(Long reviewId, String status) - admin approve/reject

Repository: /d/Desktop/WebThoiTrang/src/main/java/com/fashionshop/repository/ReviewRepository.java
- existsByUserIdAndProductId, findByProductIdAndStatus, findByProductId, findByUserId

Controller: /d/Desktop/WebThoiTrang/src/main/java/com/fashionshop/controller/ReviewController.java
- GET /api/reviews/product/{productId} - public approved only
- POST /api/reviews - create (authenticated)
- PATCH /api/reviews/admin/{id}/moderate - admin moderate

## 2. ORDER ENTITY - STATUS/COMPLETION TRACKING

Entity: /d/Desktop/WebThoiTrang/src/main/java/com/fashionshop/entity/Order.java
- Status enum: PENDING, CONFIRMED, SHIPPING, DELIVERED, CANCELLED
- Key fields: user_id, totalAmount, shippingAddress (JSONB), paymentMethod (COD|BANK_TRANSFER|MOMO)
- coupon_id, discountAmount, pointsDiscountAmount, trackingNumber, estimatedDelivery
- items: OneToMany List<OrderItem>

Service: /d/Desktop/WebThoiTrang/src/main/java/com/fashionshop/service/OrderService.java
- createOrder() validates stock, applies flash prices, coupons, loyalty points
- Stock decremented at creation

## 3. PRODUCT ENTITY

Entity: /d/Desktop/WebThoiTrang/src/main/java/com/fashionshop/entity/Product.java
- name, slug (unique), description, aiDescription, price, salePrice
- stock (Integer), status (ACTIVE|INACTIVE), category_id (ManyToOne)
- images: OneToMany List<ProductImage>
- variants: OneToMany List<ProductVariant>
- getEffectivePrice(): returns salePrice if present, else price

## 4. FRONTEND: PRODUCT-DETAIL REVIEW SECTION

File: /d/Desktop/WebThoiTrang/frontend/product-detail.html (lines 57-62, 271-316)
- loadReviews() calls api.reviews.getByProduct(productId)
- submitReview() calls api.reviews.create({productId, rating, comment})
- setRating(val) star selector 1-5
- Review form shows only if authenticated

## 5. FRONTEND: ORDERS.HTML & ORDER-DETAIL.HTML

Orders: /d/Desktop/WebThoiTrang/frontend/orders.html (169 lines)
- Status tabs: All, PENDING, CONFIRMED, SHIPPING, DELIVERED, CANCELLED
- loadOrders(params) calls api.orders.list(params)
- cancelOrder(id) for PENDING/CONFIRMED only

Detail: /d/Desktop/WebThoiTrang/frontend/order-detail.html (151 lines)
- Progress steps (lines 41-48): PENDING → CONFIRMED → SHIPPING → DELIVERED
- Items display: image, name, size, color, qty, price
- Summary: status, date, payment, coupon, totals, address

## 6. CLOUDINARY SERVICE

File: /d/Desktop/WebThoiTrang/src/main/java/com/fashionshop/service/CloudinaryService.java
- upload(MultipartFile file, String folder): validates type, size (10MB max)
- delete(String imageUrl): calls cloudinary.uploader().destroy()
- extractPublicId(imageUrl): parses URL to extract public_id
- Allowed: JPEG, PNG, WebP, GIF with q_auto,f_auto optimization

## 7. DB MIGRATION FILES (Flyway)

Location: /d/Desktop/WebThoiTrang/src/main/resources/db/migration/

V1__init.sql: Main schema
- users, categories, products, product_images, coupons
- orders (status: PENDING|CONFIRMED|SHIPPING|DELIVERED|CANCELLED, shipping_address JSONB)
- order_items (order_id, product_id, quantity, price, size, color)
- reviews (user_id, product_id, rating 1-5, comment, status: PENDING|APPROVED|REJECTED)
- cart_items, password_reset_tokens

Other: V2-V7 migrations for variants, wishlist, flash_sale, loyalty, etc

## 8. FRONTEND API CLIENT

File: /d/Desktop/WebThoiTrang/frontend/js/api.js

Reviews:
- api.reviews.getByProduct(productId)
- api.reviews.create(data), update(id, data), delete(id)
- api.reviews.adminList(params), adminModerate(id, status)

Orders:
- api.orders.create(data), list(params), get(id), cancel(id)
- api.orders.adminList(params), adminUpdateStatus(id, data)

Auth management:
- Auth.getAccess(), Auth.save(), Auth.clear()
- Auto-refresh on 401 with _refreshing promise
