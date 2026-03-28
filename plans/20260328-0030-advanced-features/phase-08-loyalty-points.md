# Phase 08 — Loyalty Points

**Complexity:** Medium
**Depends on:** Phase 07 (email notification when points expire or earned)
**Blocks:** Phase 09 (Referral grants points)

---

## Overview

Users earn points when an order is delivered (1 point = 1,000 VNĐ spent). Points can be redeemed for a coupon (auto-generated). Points expire after 12 months from earn date.

**Rules:**
- Earn: 1 point per 1,000 VNĐ on DELIVERED orders.
- Redeem: 100 points = auto-generate coupon worth 50,000 VNĐ FIXED.
- Points expire 12 months after earned.
- Cannot earn points on orders that used a coupon from points redemption.
- Cannot combine: points-generated coupons are excluded from further point earning.

---

## Key Insights

- Points balance = SUM of non-expired, non-used transactions from `loyalty_transactions`.
- No "points balance" column on users — computed from transaction log (audit trail).
- Coupon auto-generated on redeem uses existing `Coupon` entity (FIXED type, 1 max use, short expiry).
- Points are only awarded when order status reaches `DELIVERED` (not on creation).

---

## DB Tables

### Flyway: `V8__loyalty_points.sql`

```sql
CREATE TABLE IF NOT EXISTS loyalty_transactions (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    points          INTEGER      NOT NULL,           -- positive=earn, negative=redeem
    type            VARCHAR(30)  NOT NULL,           -- EARNED | REDEEMED | EXPIRED | REFERRAL_BONUS
    reference_id    BIGINT,                          -- order_id or coupon_id
    description     VARCHAR(255),
    expires_at      TIMESTAMP,                       -- NULL for debit transactions
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_loyalty_user_id ON loyalty_transactions(user_id);
CREATE INDEX idx_loyalty_type ON loyalty_transactions(type);
CREATE INDEX idx_loyalty_expires_at ON loyalty_transactions(expires_at);
```

---

## Files to Create (Backend)

| File | Purpose |
|------|---------|
| `entity/LoyaltyTransaction.java` | Transaction log entity |
| `repository/LoyaltyTransactionRepository.java` | Balance query, history |
| `dto/response/LoyaltyResponse.java` | currentPoints, history list, nextExpiry |
| `service/LoyaltyService.java` | earnPoints, redeemPoints, getBalance, expirePoints scheduler |
| `controller/LoyaltyController.java` | User-facing endpoints |
| `db/migration/V8__loyalty_points.sql` | Migration |

## Files to Modify (Backend)

| File | Change |
|------|--------|
| `service/OrderService.java` | On status → DELIVERED: call `loyaltyService.earnPoints(userId, orderAmount)` |
| `service/CouponService.java` | Mark coupons created from loyalty redemption with a flag to exclude from earning |
| `entity/Coupon.java` | Add `Boolean fromLoyalty` flag |

---

## API Endpoints

```
GET  /api/loyalty/me              — my points balance + recent transactions (User)
POST /api/loyalty/redeem          — redeem N points for a coupon (User)
GET  /api/admin/loyalty/users     — all users with point balances (ADMIN)
```

Redeem request:
```json
{ "points": 100 }
```

Redeem response:
```json
{
  "couponCode": "LOYALTY-XXXXXXXXXX",
  "discountAmount": 50000,
  "expiresAt": "2026-04-28T00:00:00"
}
```

---

## LoyaltyService Core Logic

```java
@Transactional
public void earnPoints(Long userId, BigDecimal orderAmount, Long orderId, boolean fromLoyaltyCoupon) {
    if (fromLoyaltyCoupon) return;  // No double-dipping
    int points = orderAmount.divide(BigDecimal.valueOf(1000), RoundingMode.DOWN).intValue();
    if (points <= 0) return;
    LoyaltyTransaction tx = LoyaltyTransaction.builder()
        .userId(userId).points(points).type(EARNED)
        .referenceId(orderId)
        .expiresAt(LocalDateTime.now().plusMonths(12))
        .description("Mua hàng đơn #" + orderId)
        .build();
    repo.save(tx);
}

public int getBalance(Long userId) {
    // SUM of positive points where expires_at > now or expires_at IS NULL
    // MINUS SUM of negative (redeemed) points
    return repo.computeBalance(userId, LocalDateTime.now());
}

@Scheduled(cron = "0 0 2 * * *")  // 2am daily
@Transactional
public void expirePoints() {
    // Find users with points expiring today
    // Create EXPIRED transactions to zero them out
    // Optionally send email warning 30 days before expiry
}
```

Balance query:
```java
@Query("SELECT COALESCE(SUM(t.points), 0) FROM LoyaltyTransaction t " +
       "WHERE t.userId = :userId " +
       "AND (t.expiresAt IS NULL OR t.expiresAt > :now OR t.points < 0)")
int computeBalance(@Param("userId") Long userId, @Param("now") LocalDateTime now);
```

---

## Redeem Flow

```java
@Transactional
public CouponResponse redeemPoints(Long userId, int points) {
    int balance = getBalance(userId);
    if (balance < points) throw new BadRequestException("Insufficient points");
    if (points < 100) throw new BadRequestException("Minimum redemption is 100 points");

    // Generate unique coupon code
    String code = "LOYALTY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    Coupon coupon = Coupon.builder()
        .code(code).discountType(FIXED)
        .discountValue(BigDecimal.valueOf((points / 100) * 50000L))
        .maxUses(1).minOrderAmount(BigDecimal.ZERO)
        .expiresAt(LocalDateTime.now().plusDays(30))
        .fromLoyalty(true)
        .build();
    couponRepo.save(coupon);

    // Debit transaction
    repo.save(LoyaltyTransaction.builder()
        .userId(userId).points(-points).type(REDEEMED)
        .referenceId(coupon.getId()).description("Đổi điểm lấy coupon")
        .build());

    return CouponResponse.from(coupon);
}
```

---

## Implementation Steps

1. Write `V8__loyalty_points.sql` migration.
2. Add `fromLoyalty` boolean to `Coupon` entity + migration column.
3. Create `LoyaltyTransaction` entity (no soft delete, append-only log).
4. Create `LoyaltyTransactionRepository` with balance query.
5. Create `LoyaltyService` — earn, redeem, balance, expire scheduler.
6. Create `LoyaltyController` — user and admin endpoints.
7. Modify `OrderService.updateStatus()`: when transitioning to DELIVERED, call `loyaltyService.earnPoints()`.
8. Detect if order used a loyalty coupon: check `order.getCoupon().isFromLoyalty()`.
9. Frontend: Add "Điểm thưởng" section in `profile.html` — show balance + history.
10. Frontend: Add redeem widget with points input and coupon display.
11. Frontend: Show points earned on `checkout-success.html`.

---

## Success Criteria

- [ ] Order of 500,000 VNĐ delivered → user earns 500 points.
- [ ] User with 200 points can redeem 100 points → gets coupon worth 50,000 VNĐ.
- [ ] Balance page shows accurate point history with expiry dates.
- [ ] Points expire after 12 months (scheduler test with near-expiry mock data).
- [ ] No points earned on orders using a loyalty-generated coupon.
