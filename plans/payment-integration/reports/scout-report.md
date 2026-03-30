# Payment & Order Flow Scout Report

Date: 2026-03-30
Scope: Payment integration, order creation checkout flow
Status: Complete

## 1. BACKEND ORDER MANAGEMENT (Spring Boot)

### Core Entity
Location: /d/Desktop/WebThoiTrang/src/main/java/com/fashionshop/entity/Order.java
Order Status Enum:
  - PENDING (initial state)
  - CONFIRMED
  - SHIPPING
  - DELIVERED
  - CANCELLED

### DTOs
CreateOrderRequest: /d/Desktop/WebThoiTrang/src/main/java/com/fashionshop/dto/request/CreateOrderRequest.java
  - shippingAddress (required)
  - paymentMethod (default: COD)
  - couponCode (optional)
  - notes (optional)
  - pointsToRedeem (optional)

OrderResponse: /d/Desktop/WebThoiTrang/src/main/java/com/fashionshop/dto/response/OrderResponse.java
  Returns: id, userId, userEmail, status, totalAmount, discountAmount, pointsDiscountAmount, pointsRedeemed, finalAmount, shippingAddress, paymentMethod, couponCode, notes, trackingNumber, estimatedDelivery, items, createdAt, updatedAt

### Service Layer
OrderService: /d/Desktop/WebThoiTrang/src/main/java/com/fashionshop/service/OrderService.java
  createOrder() flow:
    1. Validate cart not empty
    2. Check stock availability
    3. Calculate subtotal (with flash prices)
    4. Validate coupon if provided
    5. Redeem loyalty points if requested
    6. Create Order entity
    7. Create OrderItems
    8. Decrement stock
    9. Increment coupon usage
    10. Save Order
    11. Award loyalty points
    12. Record inventory movements
    13. Clear cart
    14. Return OrderResponse

### Controller
OrderController: /d/Desktop/WebThoiTrang/src/main/java/com/fashionshop/controller/OrderController.java
  - POST /api/orders (auth required)
  - GET /api/orders (auth required)
  - GET /api/orders/{id} (auth required)
  - POST /api/orders/{id}/cancel (auth required)
  - GET /api/orders/admin/all (ADMIN only)
  - PATCH /api/orders/admin/{id}/status (ADMIN only)

## 2. FRONTEND CHECKOUT FLOW

### HTML
Cart Page: /d/Desktop/WebThoiTrang/frontend/cart.html
  - Cart items display
  - Coupon code input
  - Loyalty points input
  - Checkout modal with:
    * Shipping address (textarea, required)
    * Payment method (dropdown: COD, BANK_TRANSFER)
    * Order notes (textarea, optional)
    * Order summary
    * Place Order button

Checkout Success: /d/Desktop/WebThoiTrang/frontend/checkout-success.html

### JavaScript API
API File: /d/Desktop/WebThoiTrang/frontend/js/api.js
Order methods:
  api.orders.create(data) -> POST /orders
  api.orders.list(params) -> GET /orders
  api.orders.get(id) -> GET /orders/{id}
  api.orders.cancel(id) -> POST /orders/{id}/cancel
  api.orders.adminList(params) -> GET /orders/admin/all
  api.orders.adminUpdateStatus(id, data) -> PATCH /orders/admin/{id}/status

Request payload:
  {
    shippingAddress: string,
    paymentMethod: "COD" | "BANK_TRANSFER",
    notes: string | null,
    couponCode: string | null,
    pointsToRedeem: int | null
  }

Auth: JWT Bearer token, auto-refresh on 401, localStorage

## 3. SECURITY CONFIGURATION

SecurityConfig: /d/Desktop/WebThoiTrang/src/main/java/com/fashionshop/security/SecurityConfig.java

Public endpoints (no auth):
  - /api/auth/**
  - /api/products/** (GET)
  - /api/categories/** (GET)
  - /api/reviews/** (GET)
  - /api/flash-sales/** (GET)
  - /api/public/security-config
  - /api/security/devtools-detected
  - /swagger-ui/**, /api-docs/**

Protected endpoints (auth required):
  - /api/orders/** (all)
  - /api/cart/**
  - Admin: hasRole('ADMIN')

JWT Config: /d/Desktop/WebThoiTrang/src/main/resources/application.yml
  - Access token: 15 minutes
  - Refresh token: 7 days
  - Secret: env variable JWT_SECRET

## 4. EXTERNAL API PATTERN

CloudinaryService: /d/Desktop/WebThoiTrang/src/main/java/com/fashionshop/service/CloudinaryService.java
Pattern:
  1. Constructor injection
  2. Environment variables for credentials
  3. Try-catch with logging
  4. Validation before API call
  5. Return clean response (URL)

Config in application.yml:
  app:
    cloudinary:
      cloud-name: env var
      api-key: env var
      api-secret: env var

Methods:
  - upload(MultipartFile, folder) -> URL
  - delete(imageUrl) -> void

## 5. PACKAGE STRUCTURE

com.fashionshop/
  config/ - AppConfig, CorsConfig, OAuth2Config, RedisConfig, OpenApiConfig, GeminiConfig
  controller/ - OrderController, ProductController, etc
  dto/request/ - CreateOrderRequest, UpdateOrderStatusRequest, etc
  dto/response/ - OrderResponse, OrderItemResponse, etc
  entity/ - Order, OrderItem, User, Product, etc
  exception/ - Custom exceptions
  repository/ - OrderRepository, OrderItemRepository, etc
  security/ - SecurityConfig, JwtAuthFilter, CustomUserDetailsService, OAuth2
  service/ - OrderService, CartService, CloudinaryService, GeminiService, LoyaltyService, CouponService, FlashSaleService, InventoryService, EmailService, etc
  util/ - Utilities

## 6. KEY FILES LISTED

Backend (Java):
  - /d/Desktop/WebThoiTrang/src/main/java/com/fashionshop/entity/Order.java
  - /d/Desktop/WebThoiTrang/src/main/java/com/fashionshop/entity/OrderItem.java
  - /d/Desktop/WebThoiTrang/src/main/java/com/fashionshop/controller/OrderController.java
  - /d/Desktop/WebThoiTrang/src/main/java/com/fashionshop/service/OrderService.java
  - /d/Desktop/WebThoiTrang/src/main/java/com/fashionshop/dto/request/CreateOrderRequest.java
  - /d/Desktop/WebThoiTrang/src/main/java/com/fashionshop/dto/response/OrderResponse.java
  - /d/Desktop/WebThoiTrang/src/main/java/com/fashionshop/security/SecurityConfig.java
  - /d/Desktop/WebThoiTrang/src/main/java/com/fashionshop/service/CloudinaryService.java
  - /d/Desktop/WebThoiTrang/src/main/resources/application.yml

Frontend (HTML/JS):
  - /d/Desktop/WebThoiTrang/frontend/cart.html
  - /d/Desktop/WebThoiTrang/frontend/checkout-success.html
  - /d/Desktop/WebThoiTrang/frontend/js/api.js

## 7. ORDER STATUS TRANSITIONS

PENDING -> CONFIRMED or CANCELLED
CONFIRMED -> SHIPPING or CANCELLED
SHIPPING -> DELIVERED only
CANCELLED and DELIVERED are terminal states

## 8. ORDER CREATION SUMMARY

Request:
  POST /api/orders (with JWT auth)
  {
    shippingAddress: "123 Main St",
    paymentMethod: "COD",
    couponCode: "SAVE20",
    pointsToRedeem: 50,
    notes: "Please leave at door"
  }

Response:
  {
    success: true,
    message: "Order created successfully",
    data: {
      id: 123,
      status: "PENDING",
      totalAmount: 1500000,
      discountAmount: 300000,
      pointsDiscountAmount: 50000,
      finalAmount: 1150000,
      shippingAddress: "123 Main St",
      paymentMethod: "COD",
      items: [...]
    }
  }

## 9. MISSING: PAYMENT PROCESSING

Current Issue:
  - Orders created with paymentMethod field
  - NO actual payment processing implemented
  - Status PENDING until admin manually updates
  - NO IPN/webhook endpoint for payment callbacks
  - NO payment gateway integration (Stripe, MoMo, VNPay, etc)

Questions:
  1. Which payment provider to integrate?
  2. Where to add public IPN webhook?
  3. Refund policy for cancelled orders?
  4. BANK_TRANSFER: Manual or auto-verified?
