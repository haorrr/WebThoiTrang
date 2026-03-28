# Phase 02 — Wishlist

**Complexity:** Easy
**Depends on:** Phase 01 (ProductVariant entity exists)
**Blocks:** Phase 07 (email automation sends wishlist price-drop alerts)

---

## Overview

Users save products (optionally a specific variant) to their wishlist. When a product's price drops or a flash sale starts, users get notified (email — handled in Phase 07). The wishlist is private per user.

---

## Key Insights

- No "shared wishlist" or "public wishlist" needed (YAGNI).
- A user can wishlist the same product only once (unique constraint).
- Price-drop notification trigger is in Phase 07 (email automation).
- Frontend: heart icon toggle on product cards and product detail page.

---

## New DB Table

### Flyway: `V4__wishlist.sql`

```sql
CREATE TABLE IF NOT EXISTS wishlist_items (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id  BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, product_id)
);

CREATE INDEX idx_wishlist_user_id ON wishlist_items(user_id);
CREATE INDEX idx_wishlist_product_id ON wishlist_items(product_id);
```

Note: No `deleted_at` — wishlist items are hard deleted (no soft delete needed).

---

## Files to Create (Backend)

| File | Purpose |
|------|---------|
| `entity/WishlistItem.java` | JPA entity (no BaseEntity — no soft delete) |
| `repository/WishlistRepository.java` | findByUserId, existsByUserIdAndProductId, deleteByUserIdAndProductId |
| `dto/response/WishlistResponse.java` | Contains List<ProductSummaryResponse> |
| `service/WishlistService.java` | add, remove, getByUser, isWishlisted(userId, productId) |
| `controller/WishlistController.java` | REST endpoints |
| `db/migration/V4__wishlist.sql` | Migration |

## Files to Modify (Frontend)

| File | Change |
|------|--------|
| `static/products.html` | Heart icon on product cards; toggle via JS |
| `static/product-detail.html` | Wishlist button; show if already wishlisted |
| `static/wishlist.html` | **NEW** — dedicated wishlist page |
| `static/js/api.js` | Add wishlist API calls |
| `static/script.js` | Update nav to show wishlist link |

---

## API Endpoints

```
GET    /api/wishlist          — get my wishlist (User)
POST   /api/wishlist/{productId}   — add to wishlist (User)
DELETE /api/wishlist/{productId}   — remove from wishlist (User)
GET    /api/wishlist/check/{productId} — is product in my wishlist? (User) → {wishlisted: true}
```

---

## Entity Skeleton

```java
@Entity @Table(name = "wishlist_items")
public class WishlistItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
```

---

## Implementation Steps

1. Write `V4__wishlist.sql` migration.
2. Create `WishlistItem` entity (no soft delete, manual `@PrePersist`).
3. Create `WishlistRepository` — include `existsByUserIdAndProductId()` for toggle logic.
4. Create `WishlistResponse` wrapping `List<ProductSummaryResponse>`.
5. Create `WishlistService` — toggle (add if not exists, remove if exists), getByUser.
6. Create `WishlistController` — map to `/api/wishlist`.
7. Add `SecurityConfig` rules (all wishlist endpoints require auth).
8. Frontend: Add heart icon SVG to product card in `products.html`; call check API on page load.
9. Frontend: `wishlist.html` — list products with remove button and "Add to Cart" shortcut.
10. Frontend: In `product-detail.html`, add wishlist toggle button.

---

## Success Criteria

- [ ] User can add/remove products from wishlist.
- [ ] Heart icon on product list reflects current wishlist state.
- [ ] Duplicate add returns 200 (idempotent toggle, not error).
- [ ] Wishlist page shows product image, name, price, with remove and add-to-cart buttons.

---

## Security Considerations

- All wishlist endpoints require `Authentication` — extract userId from JWT.
- Users can only see/modify their own wishlist (userId from JWT, not request param).
