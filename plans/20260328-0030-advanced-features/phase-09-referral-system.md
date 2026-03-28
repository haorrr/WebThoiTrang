# Phase 09 — Referral System

**Complexity:** Medium
**Depends on:** Phase 08 (referral bonus credited as loyalty points)
**Blocks:** Nothing

---

## Overview

Each user gets a unique referral code. When a new user registers with a referral code and places their first order, both the referrer and the new user receive a benefit:
- **New user:** auto-applied 50,000 VNĐ FIXED coupon on first order.
- **Referrer:** 200 loyalty points credited when referred user's first order is DELIVERED.

Simple, no MLM chain — only 1 level deep.

---

## Key Insights

- Referral code = deterministic short hash of `userId` (e.g., `FS-` + base36 of userId padded to 6 chars). No separate table needed for codes.
- Track referral relationship in `referrals` table to prevent double-rewarding.
- "First order" = `COUNT(orders WHERE userId = X AND status != CANCELLED) = 1` at time of DELIVERED.
- Referral coupon uses existing `Coupon` entity (add `fromReferral` flag).

---

## DB Table

### Flyway: `V9__referral.sql`

```sql
-- Add referral tracking to users
ALTER TABLE users ADD COLUMN IF NOT EXISTS referral_code VARCHAR(20) UNIQUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS referred_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL;

-- Track reward status
CREATE TABLE IF NOT EXISTS referrals (
    id                  BIGSERIAL PRIMARY KEY,
    referrer_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    referred_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    referrer_rewarded   BOOLEAN NOT NULL DEFAULT FALSE,  -- 200 points credited?
    referred_rewarded   BOOLEAN NOT NULL DEFAULT FALSE,  -- coupon sent?
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (referred_id)  -- one referrer per new user
);

CREATE INDEX idx_referrals_referrer_id ON referrals(referrer_id);
```

---

## Files to Create (Backend)

| File | Purpose |
|------|---------|
| `entity/Referral.java` | Referral tracking entity |
| `repository/ReferralRepository.java` | findByReferredId, findByReferrerId |
| `service/ReferralService.java` | generateCode, applyReferral, processRewards |
| `controller/ReferralController.java` | Get my code, referral stats |
| `db/migration/V9__referral.sql` | Migration |

## Files to Modify (Backend)

| File | Change |
|------|--------|
| `entity/User.java` | Add `referralCode`, `referredByUserId` fields |
| `entity/Coupon.java` | Add `fromReferral` boolean flag |
| `service/AuthService.java` | On register: generate referral code; if `referralCode` param provided, look up referrer and create Referral record; issue new-user coupon |
| `dto/request/RegisterRequest.java` | Add optional `referralCode` field |
| `service/OrderService.java` | On order DELIVERED: check if first order → `referralService.processReferrerReward()` |
| `dto/response/UserResponse.java` | Add `referralCode`, `referralUrl` fields |

---

## API Endpoints

```
GET  /api/referral/my-code        — get my referral code + link (User)
GET  /api/referral/stats          — how many referred, rewards earned (User)
```

No admin endpoints needed (YAGNI).

Registration:
```
POST /api/auth/register  { ..., "referralCode": "FS-ABC123" }
```

---

## Referral Code Generation

```java
// In AuthService.register(), after saving user:
String code = "FS-" + Long.toString(user.getId(), 36).toUpperCase();
// e.g., userId=12345 → "FS-9IX"
// Pad to ensure minimum length: if < 4 chars, left-pad with zeros
user.setReferralCode(code);
```

---

## Registration Flow with Referral

```java
// AuthService.register():
if (req.getReferralCode() != null) {
    User referrer = userRepo.findByReferralCode(req.getReferralCode())
        .orElse(null);  // silent fail if invalid code
    if (referrer != null && !referrer.getId().equals(newUser.getId())) {
        newUser.setReferredByUserId(referrer.getId());
        referralRepo.save(new Referral(referrer, newUser));
        // Issue new-user coupon immediately
        String couponCode = "WELCOME-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Coupon coupon = Coupon.builder()
            .code(couponCode).discountType(FIXED)
            .discountValue(BigDecimal.valueOf(50000))
            .maxUses(1).minOrderAmount(BigDecimal.ZERO)
            .expiresAt(LocalDateTime.now().plusDays(30))
            .fromReferral(true)
            .build();
        couponRepo.save(coupon);
        // Email new user with coupon code
        emailService.sendReferralWelcomeEmail(newUser.getEmail(), couponCode);
    }
}
```

## First-Order Reward for Referrer

```java
// OrderService.updateStatus() on DELIVERED:
referralService.processReferrerReward(order.getUser().getId(), order.getId());

// ReferralService:
public void processReferrerReward(Long referredUserId, Long orderId) {
    // Check if first delivered order
    long deliveredCount = orderRepo.countByUserIdAndStatus(referredUserId, DELIVERED);
    if (deliveredCount != 1) return;

    Referral referral = referralRepo.findByReferredId(referredUserId).orElse(null);
    if (referral == null || referral.isReferrerRewarded()) return;

    loyaltyService.creditReferralBonus(referral.getReferrerId(), 200, referredUserId);
    referral.setReferrerRewarded(true);
    referralRepo.save(referral);
}
```

---

## Implementation Steps

1. Write `V9__referral.sql` migration.
2. Add `fromReferral` to `Coupon` entity + migration column.
3. Add `referralCode`, `referredByUserId` to `User` entity.
4. Create `Referral` entity + repository.
5. Create `ReferralService` — code generation, applyReferral, processReferrerReward.
6. Update `AuthService.register()` — generate code + handle referral code input.
7. Add `referralCode` to `RegisterRequest` DTO (optional).
8. Update `OrderService` to call `referralService.processReferrerReward()` on DELIVERED.
9. Create `ReferralController` — `/api/referral/my-code` and `/api/referral/stats`.
10. Add `sendReferralWelcomeEmail()` to `EmailService`.
11. Frontend: Add referral code sharing widget in `profile.html` — show code + copy button + share link.
12. Frontend: Show referral stats (how many referred, points earned from referrals).
13. Frontend: Add referral code field on `register.html` (pre-filled if URL has `?ref=CODE`).

---

## Success Criteria

- [ ] Each user has a unique referral code visible on profile page.
- [ ] New user registers with valid referral code → gets 50,000 VNĐ coupon via email.
- [ ] After new user's first order is DELIVERED → referrer gets 200 loyalty points.
- [ ] Invalid referral code on register → silently ignored, registration succeeds.
- [ ] Referrer cannot refer themselves (code matches their own userId).
- [ ] Referrer reward credited only once per referred user.
