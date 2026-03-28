# Phase 01 — Product Variants (Size/Color Filter)

**Complexity:** Medium
**Depends on:** Nothing (foundational)
**Blocks:** Phase 02, 03, 04, 05, 10

---

## Overview

Currently `size` and `color` are free-text strings on `cart_items` and `order_items`. No variant-level stock tracking exists — the entire product has a single `stock` integer. This phase introduces a `product_variants` table that tracks stock per size/color combination.

**Why foundational:** Flash Sale needs variant-level price override. Wishlist tracks a variant. Quick Buy selects a variant. Inventory management tracks variant stock. Without this, all subsequent phases would add complexity on top of the broken existing model.

---

## Key Insights

- `Product.stock` will be **deprecated** but kept for backward compat (set to SUM of variant stocks via trigger or computed at read time).
- Existing cart/order data references `size`/`color` strings — these remain as snapshots (no migration of historical data needed).
- Filter on product list page by `size` and `color` requires a JOIN to `product_variants`.

---

## New DB Tables

### Flyway: `V3__product_variants.sql`

```sql
CREATE TABLE IF NOT EXISTS product_variants (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT       NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    size        VARCHAR(20),          -- XS, S, M, L, XL, XXL, or numeric
    color       VARCHAR(50),          -- hex or name
    color_code  VARCHAR(10),          -- #FFFFFF for swatch display
    sku         VARCHAR(100) UNIQUE,  -- optional barcode/SKU
    stock       INTEGER      NOT NULL DEFAULT 0 CHECK (stock >= 0),
    price_adjustment DECIMAL(15,2) NOT NULL DEFAULT 0, -- +/- from base price
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP,
    UNIQUE (product_id, size, color)
);

CREATE INDEX idx_product_variants_product_id ON product_variants(product_id);
CREATE INDEX idx_product_variants_size ON product_variants(size);
CREATE INDEX idx_product_variants_color ON product_variants(color);
```

---

## Files to Create (Backend)

| File | Purpose |
|------|---------|
| `entity/ProductVariant.java` | JPA entity |
| `repository/ProductVariantRepository.java` | Queries: findByProductId, findByProductIdAndSizeAndColor |
| `dto/request/ProductVariantRequest.java` | Create/update variant DTO |
| `dto/response/ProductVariantResponse.java` | Response DTO with static `from()` |
| `service/ProductVariantService.java` | CRUD + stock management |
| `controller/ProductVariantController.java` | REST endpoints |
| `repository/ProductSpecification.java` | **MODIFY** — add size/color filter predicates |

## Files to Modify (Backend)

| File | Change |
|------|--------|
| `entity/Product.java` | Add `@OneToMany List<ProductVariant> variants` |
| `dto/response/ProductResponse.java` | Add `List<ProductVariantResponse> variants` |
| `dto/response/ProductSummaryResponse.java` | Add available sizes/colors as `Set<String>` |
| `service/CartService.java` | Validate variant stock instead of product stock |
| `service/OrderService.java` | Deduct variant stock; fall back to product stock if no variants |
| `db/migration/V3__product_variants.sql` | New migration file |

## Files to Create (Frontend)

| File | Purpose |
|------|---------|
| `static/js/variants.js` | Variant selector component (size buttons, color swatches) |

## Files to Modify (Frontend)

| File | Change |
|------|--------|
| `static/product-detail.html` | Add size/color selector UI, update add-to-cart to send variantId |
| `static/products.html` | Add size/color filter checkboxes in sidebar |
| `static/js/api.js` | Add variant API calls |
| `static/admin/products.html` | Add variant management tab |

---

## API Endpoints

```
GET  /api/products/{id}/variants          — list variants (public)
POST /api/products/{id}/variants          — create variant (ADMIN)
PUT  /api/products/{id}/variants/{vid}    — update variant (ADMIN)
DELETE /api/products/{id}/variants/{vid}  — soft-delete variant (ADMIN)
PATCH /api/products/{id}/variants/{vid}/stock — adjust stock (ADMIN)
```

Product list filter additions:
```
GET /api/products?size=M&color=Black&...  — multi-value params
```

---

## Entity Skeleton

```java
@Entity @Table(name = "product_variants")
@Where(clause = "deleted_at IS NULL")
public class ProductVariant extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    private String size;
    private String color;
    private String colorCode;
    private String sku;
    @Column(nullable = false)
    private Integer stock = 0;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal priceAdjustment = BigDecimal.ZERO;
}
```

---

## CartService Changes

```java
// When adding to cart:
// 1. If variantId provided → validate ProductVariant.stock
// 2. If no variants exist for product → fallback to Product.stock
// 3. Store variantId on CartItem (add column cart_items.variant_id FK)
```

Add to `V3__product_variants.sql`:
```sql
ALTER TABLE cart_items ADD COLUMN variant_id BIGINT REFERENCES product_variants(id) ON DELETE SET NULL;
```

---

## Implementation Steps

1. Write `V3__product_variants.sql` migration.
2. Create `ProductVariant` entity extending `BaseEntity`.
3. Create `ProductVariantRepository` with `findByProductIdAndSizeAndColor()`.
4. Create `ProductVariantRequest` / `ProductVariantResponse` DTOs.
5. Create `ProductVariantService` — CRUD, stock validation logic.
6. Create `ProductVariantController` under `/api/products/{id}/variants`.
7. Add `variants` list to `Product` entity (bidirectional OneToMany).
8. Update `ProductResponse.from()` to include variants.
9. Update `ProductSummaryResponse.from()` to include distinct sizes and colors.
10. Update `ProductSpecification.withFilters()` to JOIN variants for size/color predicates.
11. Update `CartService` to use variant stock when `variantId` is present.
12. Update `OrderService` similarly.
13. Add `SecurityConfig` permit rules for `GET /api/products/*/variants`.
14. Frontend: `variants.js` component — size button group + color swatches.
15. Frontend: Update `product-detail.html` to render variants, pass `variantId` to cart API.
16. Frontend: Add filter checkboxes in `products.html` sidebar.
17. Admin: Variant management in `admin/products.html`.

---

## Success Criteria

- [ ] Can create product with 3 size variants, each with independent stock.
- [ ] Adding to cart deducts from correct variant's stock.
- [ ] Product list filter by `size=M` returns only products that have M variant in stock.
- [ ] Admin can view/edit/delete individual variants.
- [ ] Migration runs cleanly on existing DB (no data loss).

---

## Risk Assessment

| Risk | Mitigation |
|------|-----------|
| Breaking existing cart/order data that uses string size/color | Keep `size`/`color` strings on cart_items/order_items as-is; variant_id is nullable |
| Product.stock becoming stale | Compute total stock = SUM(variants.stock) on ProductResponse; keep Product.stock for products with no variants |
| N+1 on product list with variants | Add `@EntityGraph` for variant fetch only on product detail, not list view |
