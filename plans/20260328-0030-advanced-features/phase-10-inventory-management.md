# Phase 10 — Inventory Management

**Complexity:** Medium
**Depends on:** Phase 01 (ProductVariant tracks stock per variant)
**Blocks:** Phase 11 (inventory data feeds analytics dashboard)

---

## Overview

Admin-facing feature: track stock movements (restock, adjustments, order deductions), set low-stock alert thresholds per product/variant, and view stock history. Alert emails when stock drops below threshold.

**Current state:** Stock is decremented in `OrderService` with no history log. `DashboardService` already queries low-stock products with threshold ≤ 5.

---

## Key Insights

- `inventory_transactions` is an append-only ledger. Actual stock remains on `Product.stock` and `ProductVariant.stock` (source of truth). Ledger is for auditing.
- Do NOT compute stock from ledger — this would require replaying all transactions. Keep stock on entity, ledger is supplementary.
- Low-stock threshold is per-product (default 5, configurable by admin).
- Email alert uses existing `EmailService`. Alert email sent max once per 24h per product (use `email_log`).

---

## DB Tables

### Flyway: `V10__inventory.sql`

```sql
-- Stock movement ledger
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT       NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    variant_id      BIGINT       REFERENCES product_variants(id) ON DELETE SET NULL,
    quantity_change INTEGER      NOT NULL,   -- positive=restock, negative=sale/adjustment
    type            VARCHAR(30)  NOT NULL,   -- RESTOCK | SALE | ADJUSTMENT | RETURN
    reference_id    BIGINT,                  -- order_id for SALE/RETURN
    note            TEXT,
    performed_by    BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Low-stock alert threshold per product
ALTER TABLE products ADD COLUMN IF NOT EXISTS stock_alert_threshold INTEGER NOT NULL DEFAULT 5;

CREATE INDEX idx_inventory_product_id ON inventory_transactions(product_id);
CREATE INDEX idx_inventory_created_at ON inventory_transactions(created_at DESC);
```

---

## Files to Create (Backend)

| File | Purpose |
|------|---------|
| `entity/InventoryTransaction.java` | Ledger entity (no soft delete) |
| `repository/InventoryTransactionRepository.java` | findByProductId, restock history |
| `dto/request/RestockRequest.java` | Admin restock: productId, variantId, quantity, note |
| `dto/response/InventoryTransactionResponse.java` | Response DTO |
| `service/InventoryService.java` | recordSale, recordRestock, recordAdjustment, getHistory, lowStockAlert |
| `controller/InventoryController.java` | Admin endpoints |
| `db/migration/V10__inventory.sql` | Migration |

## Files to Modify (Backend)

| File | Change |
|------|--------|
| `service/OrderService.java` | On order created: call `inventoryService.recordSale()` per item. On cancel: call `inventoryService.recordReturn()`. |
| `service/ProductVariantService.java` | On stock adjustment from admin: call `inventoryService.recordAdjustment()` |
| `entity/Product.java` | Add `stockAlertThreshold` field |
| `dto/request/ProductRequest.java` | Add `stockAlertThreshold` field |

## Files to Create (Frontend)

| File | Purpose |
|------|---------|
| `static/admin/inventory.html` | Inventory management page |

## Files to Modify (Frontend)

| File | Change |
|------|--------|
| `static/admin/products.html` | Add "Restock" button + threshold setting |
| `static/admin/dashboard.html` | Enhance low-stock widget with link to inventory page |

---

## API Endpoints

```
GET  /api/admin/inventory                          — stock overview, filter by low-stock (ADMIN)
GET  /api/admin/inventory/transactions             — full ledger (filter by product, type, date range) (ADMIN)
GET  /api/admin/inventory/products/{id}/history    — product stock history (ADMIN)
POST /api/admin/inventory/restock                  — add stock (ADMIN)
POST /api/admin/inventory/adjust                   — manual adjustment (ADMIN)
PATCH /api/admin/products/{id}/stock-threshold     — set alert threshold (ADMIN)
```

---

## InventoryService Core

```java
@Transactional
public void recordSale(Long productId, Long variantId, int quantity, Long orderId) {
    saveTransaction(productId, variantId, -quantity, SALE, orderId, null, null);
    // Stock already decremented in OrderService — no double deduction
    checkLowStockAlert(productId, variantId);
}

@Transactional
public void recordRestock(Long adminId, Long productId, Long variantId, int quantity, String note) {
    // Increment stock on Product or ProductVariant
    if (variantId != null) {
        ProductVariant v = variantRepo.findById(variantId).orElseThrow();
        v.setStock(v.getStock() + quantity);
        variantRepo.save(v);
    } else {
        Product p = productRepo.findById(productId).orElseThrow();
        p.setStock(p.getStock() + quantity);
        productRepo.save(p);
    }
    saveTransaction(productId, variantId, quantity, RESTOCK, null, note, adminId);
}

private void checkLowStockAlert(Long productId, Long variantId) {
    Product p = productRepo.findById(productId).orElse(null);
    if (p == null) return;
    int currentStock = variantId != null
        ? variantRepo.findById(variantId).map(ProductVariant::getStock).orElse(0)
        : p.getStock();
    if (currentStock <= p.getStockAlertThreshold()) {
        // Send alert email to admin (check email_log for 24h dedup)
        emailService.sendLowStockAlert("admin@fashionshop.com", p.getName(), currentStock);
    }
}
```

---

## Implementation Steps

1. Write `V10__inventory.sql` migration.
2. Create `InventoryTransaction` entity + repository.
3. Create `RestockRequest` / `InventoryTransactionResponse` DTOs.
4. Create `InventoryService` with all transaction recording methods.
5. Update `OrderService.createOrder()` to call `inventoryService.recordSale()` per item.
6. Update `OrderService.cancelOrder()` to call `inventoryService.recordReturn()` per item.
7. Create `InventoryController` — admin endpoints.
8. Add `sendLowStockAlert()` email template in `EmailService`.
9. Add `stockAlertThreshold` to `Product` entity and `ProductRequest` DTO.
10. Frontend: `admin/inventory.html` — sortable table of products with stock levels, color-coded (red = out, orange = low, green = ok).
11. Frontend: Restock modal with quantity input and note.
12. Frontend: Transaction history modal per product.

---

## Success Criteria

- [ ] Order creation inserts SALE transaction records per item.
- [ ] Admin restock updates product stock and logs RESTOCK transaction.
- [ ] Product with threshold=5 and stock=3 triggers alert email to admin.
- [ ] Alert email not re-sent within 24h for same product.
- [ ] Inventory page shows color-coded stock status for all products.
- [ ] Transaction history filterable by type and date range.
