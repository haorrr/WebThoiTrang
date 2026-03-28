# Phase 07 — Email Automation

**Complexity:** Medium
**Depends on:** Phase 06 (order status history triggers emails); Phase 02 (wishlist for price-drop emails)
**Blocks:** Nothing

---

## Overview

Extend the existing `EmailService` (which currently only sends password-reset emails) with 4 automated email types:

1. **Order status change** — triggered on each status transition.
2. **Abandoned cart** — 1 hour after last cart update with no order, send reminder.
3. **Birthday** — daily scheduler checks users with birthday today.
4. **Wishlist price drop** — when flash sale activates for a wishlisted product.

All emails are async (`@Async`) following the existing pattern.

---

## Key Insights

- `@Scheduled` tasks already enabled after Phase 03 (`@EnableScheduling`).
- `EmailService` already has `JavaMailSender`, template pattern established.
- Abandoned cart uses Redis to track "cart-changed-at" timestamps — no DB table.
- Birthday email requires `date_of_birth` column on `users` table.
- Wishlist price-drop email is triggered by `FlashSaleService.activateScheduledSales()`.

---

## DB Changes

### Flyway: `V7__email_automation.sql`

```sql
-- Birthday support
ALTER TABLE users ADD COLUMN IF NOT EXISTS date_of_birth DATE;

-- Email log (prevent duplicate sends)
CREATE TABLE IF NOT EXISTS email_log (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email_type  VARCHAR(50)  NOT NULL,  -- ORDER_STATUS | ABANDONED_CART | BIRTHDAY | PRICE_DROP
    reference_id VARCHAR(100),          -- orderId or productId
    sent_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_email_log_user_type ON email_log(user_id, email_type, sent_at);
```

---

## Files to Create (Backend)

| File | Purpose |
|------|---------|
| `entity/EmailLog.java` | Prevent duplicate emails |
| `repository/EmailLogRepository.java` | existsByUserIdAndEmailTypeAndReferenceIdAndSentAtAfter |
| `service/AbandonedCartService.java` | Scheduler that finds carts idle >1h and sends email |
| `db/migration/V7__email_automation.sql` | Migration |

## Files to Modify (Backend)

| File | Change |
|------|--------|
| `entity/User.java` | Add `dateOfBirth` field (`LocalDate`) |
| `service/EmailService.java` | Add: `sendOrderStatusEmail()`, `sendAbandonedCartEmail()`, `sendBirthdayEmail()`, `sendPriceDropEmail()` |
| `service/OrderService.java` | Call `emailService.sendOrderStatusEmail()` after status update |
| `service/CartService.java` | On cart modify, push `cartUpdatedAt:{userId}` to Redis |
| `service/FlashSaleService.java` | On sale activation, find wishlist items for flash products and send price-drop emails |
| `dto/request/UpdateProfileRequest.java` | Add `dateOfBirth` |
| `dto/response/UserResponse.java` | Add `dateOfBirth` |

---

## Email Types & Templates

### 1. Order Status Email

Triggered: `OrderService.updateStatus()` after save.

```java
@Async
public void sendOrderStatusEmail(String toEmail, String customerName,
                                  String orderId, String status, String trackingNumber) {
    // Template: "Đơn hàng #{orderId} đã {status}"
    // Include: status stepper visual, tracking number if SHIPPING
    // Subject: "FashionShop — Cập nhật đơn hàng #12345"
}
```

### 2. Abandoned Cart Email

Scheduler (every 15 min checks):
```java
@Scheduled(fixedDelay = 900_000)  // 15 minutes
public void checkAbandonedCarts() {
    // Get all Redis keys "cart:active:{userId}" with value = lastUpdatedAt
    // If now - lastUpdatedAt > 1h AND user has items in cart → send email
    // Log to email_log to prevent re-send within 24h
}
```

Cart update hook in `CartService`:
```java
// On addItem/updateItem: redisTemplate.opsForValue().set("cart:active:" + userId,
//    Instant.now().toString(), 25, TimeUnit.HOURS);
// On createOrder/clearCart: redisTemplate.delete("cart:active:" + userId);
```

### 3. Birthday Email

```java
@Scheduled(cron = "0 0 8 * * *")  // 8am daily
public void sendBirthdayEmails() {
    LocalDate today = LocalDate.now();
    List<User> users = userRepository.findByDateOfBirth(today.getMonthValue(), today.getDayOfMonth());
    // Check email_log: not sent today → send + log
}
```

Query needed in `UserRepository`:
```java
@Query("SELECT u FROM User u WHERE MONTH(u.dateOfBirth) = :month AND DAY(u.dateOfBirth) = :day AND u.deletedAt IS NULL AND u.status = 'ACTIVE'")
List<User> findByBirthday(@Param("month") int month, @Param("day") int day);
```

### 4. Wishlist Price-Drop Email

In `FlashSaleService.activateScheduledSales()`:
```java
// After setting sale ACTIVE:
// For each product in flash sale:
//   Get all wishlist_items for that product
//   For each userId: sendPriceDropEmail (check email_log first)
```

---

## Anti-Spam (EmailLog)

Before sending any email:
```java
boolean alreadySent = emailLogRepository
    .existsByUserIdAndEmailTypeAndReferenceIdAndSentAtAfter(
        userId, emailType, referenceId, LocalDateTime.now().minusHours(24));
if (!alreadySent) {
    emailService.sendXxx(...);
    emailLogRepository.save(new EmailLog(userId, emailType, referenceId));
}
```

---

## Implementation Steps

1. Write `V7__email_automation.sql` migration.
2. Add `dateOfBirth` to `User` entity + DTOs.
3. Create `EmailLog` entity + repository.
4. Add `sendOrderStatusEmail()` to `EmailService` with HTML template.
5. Call it in `OrderService.updateStatus()`.
6. Add Redis cart-active tracking in `CartService` (add/update/delete operations).
7. Create `AbandonedCartService` with `@Scheduled` + email send logic.
8. Add `sendAbandonedCartEmail()` template to `EmailService`.
9. Add `sendBirthdayEmail()` + scheduler in `UserService` or dedicated `BirthdayEmailService`.
10. Add `UserRepository.findByBirthday()` query.
11. In `FlashSaleService`, after activating sale, query wishlist and send price-drop emails.
12. Add `sendPriceDropEmail()` to `EmailService`.
13. Frontend: Add date-of-birth field to `profile.html`.

---

## Success Criteria

- [ ] Status change to CONFIRMED sends email within 5s.
- [ ] Cart idle for 1h triggers abandoned cart email (test by mocking time or reducing threshold).
- [ ] Birthday email sent at 8am only once per day per user.
- [ ] Wishlist price-drop email sent when flash sale activates.
- [ ] No duplicate emails within 24h (email_log check works).
- [ ] All emails have consistent HTML template matching FashionShop branding.

---

## Risk Assessment

| Risk | Mitigation |
|------|-----------|
| Sending emails to inactive/deleted users | Filter by `status = ACTIVE AND deleted_at IS NULL` |
| SMTP rate limits from Gmail | Emails are queued async; for production switch to SendGrid/SES |
| Abandoned cart scanner scanning all Redis keys | Use `SCAN` with pattern `cart:active:*`; or maintain Set of active cart users |
