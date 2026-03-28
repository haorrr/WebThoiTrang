# Phase 06 — Order Tracking (Enhanced)

**Complexity:** Easy
**Depends on:** Existing Order state machine (`PENDING→CONFIRMED→SHIPPING→DELIVERED`)
**Blocks:** Phase 07 (email automation sends status change emails)

---

## Overview

Add a visual progress stepper on the order detail page. Add a `tracking_notes` field on orders so admin can add shipping carrier + tracking number. No real-time WebSocket — polling every 30s is sufficient for a small shop.

**Current state:** `GET /api/orders/{id}` returns order with status string. Frontend shows status as plain text.
**Target:** Frontend renders a 4-step progress bar; admin can attach tracking info.

---

## Key Insights

- No external shipping API integration (YAGNI). Admin manually updates status and tracking info.
- "Real-time" = frontend polls every 30s on the order detail page. Simple, no SSE/WebSocket needed.
- Tracking history = append-only log of status changes with timestamps. Stored in new table.

---

## DB Changes

### Flyway: `V6__order_tracking.sql`

```sql
-- Add tracking fields to orders
ALTER TABLE orders ADD COLUMN IF NOT EXISTS tracking_number VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS carrier VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS tracking_url VARCHAR(500);

-- Status change history
CREATE TABLE IF NOT EXISTS order_status_history (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT       NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    status      VARCHAR(30)  NOT NULL,
    note        TEXT,
    changed_by  BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_status_history_order_id ON order_status_history(order_id);
```

---

## Files to Create (Backend)

| File | Purpose |
|------|---------|
| `entity/OrderStatusHistory.java` | Status change log entity |
| `repository/OrderStatusHistoryRepository.java` | findByOrderIdOrderByCreatedAtAsc |
| `dto/response/OrderStatusHistoryResponse.java` | status, note, changedAt |
| `db/migration/V6__order_tracking.sql` | Migration |

## Files to Modify (Backend)

| File | Change |
|------|--------|
| `entity/Order.java` | Add `trackingNumber`, `carrier`, `trackingUrl` fields |
| `dto/request/UpdateOrderStatusRequest.java` | Add `note`, `trackingNumber`, `carrier`, `trackingUrl` |
| `dto/response/OrderResponse.java` | Add tracking fields + `List<OrderStatusHistoryResponse>` |
| `service/OrderService.java` | On status update: save `OrderStatusHistory` record; update tracking fields |

## Files to Modify (Frontend)

| File | Change |
|------|--------|
| `static/order-detail.html` | Add 4-step progress stepper + tracking info section + auto-refresh |
| `static/orders.html` | Show tracking number if SHIPPING status |
| `static/admin/orders.html` | Add tracking number/carrier/note input in status update form |

---

## API Changes

`PATCH /api/orders/admin/{id}/status` request body expanded:
```json
{
  "status": "SHIPPING",
  "note": "Đã giao cho đơn vị vận chuyển",
  "trackingNumber": "VNP123456789",
  "carrier": "VNPost",
  "trackingUrl": "https://vnpost.vn/tracking?id=VNP123456789"
}
```

`GET /api/orders/{id}` response includes:
```json
{
  "trackingNumber": "VNP123456789",
  "carrier": "VNPost",
  "trackingUrl": "...",
  "statusHistory": [
    {"status": "PENDING", "note": null, "changedAt": "..."},
    {"status": "CONFIRMED", "note": "Đã xác nhận", "changedAt": "..."}
  ]
}
```

---

## Frontend Progress Stepper

```html
<!-- 4 steps: Đặt hàng → Xác nhận → Đang giao → Hoàn thành -->
<div class="order-stepper">
  <div class="step completed">Đặt hàng</div>
  <div class="step active">Xác nhận</div>
  <div class="step">Đang giao</div>
  <div class="step">Hoàn thành</div>
</div>
```

Step states derived from `order.status`:
- `PENDING` → step 1 active
- `CONFIRMED` → step 2 active
- `SHIPPING` → step 3 active
- `DELIVERED` → all steps completed
- `CANCELLED` → red cancelled banner

Auto-refresh: `setInterval(() => fetchOrder(orderId), 30000)` on order-detail page.

---

## Implementation Steps

1. Write `V6__order_tracking.sql` migration.
2. Add tracking columns to `Order` entity.
3. Create `OrderStatusHistory` entity (no soft delete, append-only).
4. Create `OrderStatusHistoryRepository`.
5. Update `UpdateOrderStatusRequest` DTO with tracking fields.
6. Update `OrderService.updateStatus()` to save history record and tracking info.
7. Update `OrderResponse` to include history list and tracking fields.
8. Update `order-detail.html` — stepper component + tracking number display + `setInterval` poll.
9. Update `admin/orders.html` — add tracking fields to status update modal.
10. Update `orders.html` — show tracking number pill when status is SHIPPING.

---

## Success Criteria

- [ ] Changing order status to SHIPPING saves history record.
- [ ] `GET /api/orders/{id}` returns full status history.
- [ ] Order detail page renders 4-step stepper with correct active step.
- [ ] Admin can set tracking number; user sees it on order detail.
- [ ] CANCELLED orders show red indicator, not stepper.
- [ ] Auto-refresh every 30s (verify via network tab).
