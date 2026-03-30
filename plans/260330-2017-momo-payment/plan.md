# MoMo Payment Integration Plan

**Created:** 2026-03-30
**Priority:** High
**Estimated effort:** 1-2 days

## Overview

Integrate MoMo e-wallet payment into the fashion-shop using MoMo's sandbox API (v2).
Flow: User chọn MoMo → tạo order → backend lấy payUrl → redirect → IPN callback → cập nhật trạng thái.

## Phases

| # | Phase | Status | File |
|---|-------|--------|------|
| 1 | Backend: MoMo Service & Controller | ✅ done | [phase-01-backend.md](phase-01-backend.md) |
| 2 | Frontend: Checkout UI + Redirect | ✅ done | [phase-02-frontend.md](phase-02-frontend.md) |

## Architecture Summary

```
User clicks "Đặt hàng" (MOMO)
    → POST /api/orders          → Order created (PENDING, paymentStatus=PENDING_PAYMENT)
    → POST /api/payment/momo/create → MoMoService.createPayment() → MoMo API → payUrl
    → Redirect to payUrl

User pays in MoMo app
    → MoMo POST /api/payment/momo/ipn  (PUBLIC endpoint, no auth)
    → Verify HMAC signature
    → resultCode=0 → Order paymentStatus=PAID, status=CONFIRMED
    → MoMo redirects user → GET /api/payment/momo/return → frontend success page
```

## Key Files

**Backend:**
- `Order.java` — add `paymentStatus`, `momoTransactionId`, `paymentUrl`
- `MoMoService.java` — new service
- `MoMoController.java` — new controller
- `V9__payment_fields.sql` — new migration
- `application.yml` — add momo config block

**Frontend:**
- `cart.html` — add MOMO option, handle redirect after order
- `payment-result.html` — new page for return URL

## Sandbox Credentials

```yaml
partnerCode: MOMO
accessKey: F8BBA842ECF85
secretKey: K951B6PE1waDMi640xX08PD3vg6EkVlz
endpoint: https://test-payment.momo.vn
```
