# Phase 03 — Flash Sale

**Complexity:** Medium
**Depends on:** Phase 01 (ProductVariant for per-variant flash sale support)
**Blocks:** Phase 05 (Quick Buy shows flash price)

---

## Overview

Flash Sale = a time-bounded discount on specific products. Admin creates a flash sale with start time, end time, and discount percentage. During the active window, product list and detail pages show the discounted price with a countdown timer. `@Scheduled` cron automatically activates/deactivates sales.

**Not** a separate payment flow — flash price is computed at cart/order time by checking active flash sales.

---

## Key Insights

- Existing `Product.salePrice` is a manual override. Flash Sale is scheduled and temporary — different concept.
- Flash sale price = `product.effectivePrice * (1 - discountPercent/100)`.
- At order creation, `OrderService` must check active flash sales and use flash price if applicable.
- Redis cache for "active flash sales" (TTL = 1 min) — avoids DB query on every product view.
- Countdown timer is purely frontend (JS `setInterval` against `endTime`).
- Admin must be able to see upcoming, active, and ended sales.

---

## New DB Table

### Flyway: `V5__flash_sale.sql`

```sql
CREATE TABLE IF NOT EXISTS flash_sales (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    discount_percent DECIMAL(5,2) NOT NULL CHECK (discount_percent > 0 AND discount_percent <= 100),
    starts_at       TIMESTAMP    NOT NULL,
    ends_at         TIMESTAMP    NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED', -- SCHEDULED | ACTIVE | ENDED | CANCELLED
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP,
    CHECK (ends_at > starts_at)
);

CREATE TABLE IF NOT EXISTS flash_sale_products (
    id            BIGSERIAL PRIMARY KEY,
    flash_sale_id BIGINT NOT NULL REFERENCES flash_sales(id) ON DELETE CASCADE,
    product_id    BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    stock_limit   INTEGER,  -- max units to sell at flash price (NULL = unlimited)
    sold_count    INTEGER   NOT NULL DEFAULT 0,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (flash_sale_id, product_id)
);

CREATE INDEX idx_flash_sales_status ON flash_sales(status);
CREATE INDEX idx_flash_sales_starts_at ON flash_sales(starts_at);
CREATE INDEX idx_flash_sale_products_flash_sale_id ON flash_sale_products(flash_sale_id);
CREATE INDEX idx_flash_sale_products_product_id ON flash_sale_products(product_id);
```

---

## Files to Create (Backend)

| File | Purpose |
|------|---------|
| `entity/FlashSale.java` | Flash sale header entity |
| `entity/FlashSaleProduct.java` | Flash sale items (join table with extra columns) |
| `repository/FlashSaleRepository.java` | findActive, findByStatus, scheduler queries |
| `repository/FlashSaleProductRepository.java` | findByProductIdAndActiveFlashSale |
| `dto/request/FlashSaleRequest.java` | Create/update DTO |
| `dto/response/FlashSaleResponse.java` | With product list and computed flash price |
| `service/FlashSaleService.java` | CRUD + price computation + scheduler |
| `controller/FlashSaleController.java` | REST endpoints |
| `db/migration/V5__flash_sale.sql` | Migration |

## Files to Modify (Backend)

| File | Change |
|------|--------|
| `service/OrderService.java` | Check active flash sale for product; use flash price |
| `service/CartService.java` | Return flash price in cart response |
| `dto/response/ProductSummaryResponse.java` | Add `flashPrice`, `flashSaleEndsAt` fields |
| `dto/response/ProductResponse.java` | Add `flashPrice`, `flashSaleEndsAt` fields |
| `FashionShopApplication.java` | Add `@EnableScheduling` |

## Files to Create (Frontend)

| File | Purpose |
|------|---------|
| `static/flash-sale.html` | Flash sale landing page with countdown |
| `static/js/countdown.js` | Reusable countdown timer component |
| `static/admin/flash-sales.html` | Admin CRUD for flash sales |

## Files to Modify (Frontend)

| File | Change |
|------|--------|
| `static/products.html` | Show flash badge + discounted price if flashPrice present |
| `static/product-detail.html` | Show countdown timer if in active flash sale |
| `static/index.html` | Add flash sale section/banner on home page |
| `static/js/api.js` | Add flash sale API calls |

---

## API Endpoints

```
GET  /api/flash-sales/active           — currently active flash sales (Public)
GET  /api/flash-sales/{id}             — flash sale detail with products (Public)
GET  /api/admin/flash-sales            — all flash sales with filter (ADMIN)
POST /api/admin/flash-sales            — create flash sale (ADMIN)
PUT  /api/admin/flash-sales/{id}       — update (ADMIN, only SCHEDULED)
DELETE /api/admin/flash-sales/{id}     — cancel/soft-delete (ADMIN)
POST /api/admin/flash-sales/{id}/products — add products to flash sale (ADMIN)
DELETE /api/admin/flash-sales/{id}/products/{pid} — remove product (ADMIN)
```

---

## Scheduler Logic

```java
@Scheduled(fixedDelay = 60_000)  // every 1 minute
public void activateScheduledSales() {
    // Find SCHEDULED sales where starts_at <= now → set ACTIVE, evict cache
    // Find ACTIVE sales where ends_at <= now → set ENDED, evict cache
}
```

Cache key: `"activeSales"` in Redis, TTL 60s.

---

## Price Computation at Order Time

```java
// In OrderService.createOrder():
// For each cart item:
//   activeFlashSale = flashSaleService.getActiveFlashSaleForProduct(product.getId());
//   if (activeFlashSale != null && stockLimitNotExceeded):
//     price = product.effectivePrice * (1 - discount/100)
//   else:
//     price = product.getEffectivePrice()
```

---

## Implementation Steps

1. Write `V5__flash_sale.sql` migration.
2. Add `@EnableScheduling` to `FashionShopApplication`.
3. Create `FlashSale` and `FlashSaleProduct` entities.
4. Create repositories with `findByStatus(ACTIVE)` queries.
5. Create `FlashSaleService` — CRUD + `getActiveFlashSaleForProduct()` + scheduler method.
6. Create `FlashSaleController` — public and admin endpoints.
7. Update `ProductSummaryResponse` / `ProductResponse` to include flash price (computed at service layer, not stored).
8. Update `OrderService` to apply flash price using `FlashSaleService`.
9. Update `CartService` to show flash price in cart items.
10. Frontend: `countdown.js` utility function `startCountdown(endTime, elementId)`.
11. Frontend: `flash-sale.html` listing page with countdowns.
12. Frontend: Update product cards in `products.html` to show flash badge.
13. Admin: `flash-sales.html` CRUD panel with date/time pickers.

---

## Success Criteria

- [ ] Admin creates flash sale with 2 products, 20% off, active in 1 minute.
- [ ] After 1 minute scheduler runs, sale status becomes ACTIVE.
- [ ] Product list shows flash badge and discounted price.
- [ ] Countdown timer displays on product detail page.
- [ ] Order created during flash sale uses flash price in order_items.
- [ ] After `ends_at`, status becomes ENDED and products revert to normal price.

---

## Risk Assessment

| Risk | Mitigation |
|------|-----------|
| Scheduler runs while transaction in progress | Use `@Transactional` in scheduler; idempotent status check |
| Flash price not applied if Redis cache stale | TTL = 60s = same as scheduler interval; worst case 1 min stale |
| Concurrent order draining stock_limit | Use `UPDATE flash_sale_products SET sold_count = sold_count + 1 WHERE sold_count < stock_limit` optimistic update |
