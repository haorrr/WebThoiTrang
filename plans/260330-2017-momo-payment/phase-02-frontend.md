# Phase 02 — Frontend: Checkout UI + Redirect

**Status:** 🔲 pending
**Dependencies:** Phase 01 must be done (backend endpoints available)

## Context

- `frontend/cart.html` — has checkout modal with `#paymentMethod` select (COD, BANK_TRANSFER)
- `frontend/js/api.js` — has `api.orders.create()` → POST /api/orders
- `frontend/checkout-success.html` — existing success page (may repurpose)
- Backend returns `{ payUrl }` from `POST /api/payment/momo/create`

## Implementation Steps

### Step 1 — Add MOMO to `cart.html` payment method selector

Locate `<select id="paymentMethod">` and add option:

```html
<select class="form-select" id="paymentMethod">
  <option value="COD">Thanh toán khi nhận hàng (COD)</option>
  <option value="BANK_TRANSFER">Chuyển khoản ngân hàng</option>
  <option value="MOMO">Ví MoMo</option>
</select>
```

Optionally add a MoMo logo/badge next to the option (visual only):
```html
<option value="MOMO">💜 Ví MoMo (thanh toán online)</option>
```

---

### Step 2 — Add MoMo info banner (shows when MOMO selected)

After the `<select>`, add a hidden info box:

```html
<div id="momoInfo" style="display:none;margin-top:8px;padding:10px 14px;background:#f3e8ff;border-radius:10px;border:1px solid #d8b4fe;font-size:0.85rem;color:#6b21a8">
  <strong>Thanh toán qua MoMo:</strong> Sau khi đặt hàng, bạn sẽ được chuyển đến trang thanh toán MoMo.
</div>
```

Show/hide on change:
```js
document.getElementById('paymentMethod').addEventListener('change', function() {
  document.getElementById('momoInfo').style.display = this.value === 'MOMO' ? 'block' : 'none';
});
```

---

### Step 3 — Update `placeOrder()` function in cart.html

Find the existing `placeOrder()` (or equivalent checkout submit handler).

**Current flow:**
1. Call `api.orders.create(payload)`
2. Show success toast
3. Redirect to `order-detail.html?id=...`

**New flow (add MoMo branch):**

```js
async function placeOrder() {
  const paymentMethod = document.getElementById('paymentMethod').value;
  const btn = document.getElementById('placeOrderBtn');
  setLoading(btn, true);

  try {
    // Step 1: Create order
    const orderRes = await api.orders.create({
      shippingAddress: document.getElementById('shippingAddress').value.trim(),
      paymentMethod,
      couponCode: appliedCoupon || null,
      notes: document.getElementById('orderNotes').value.trim() || null,
      pointsToRedeem: pointsToRedeem || null,
    });

    if (!orderRes.success) {
      showToast(orderRes.message || 'Không thể đặt hàng', 'error');
      setLoading(btn, false);
      return;
    }

    const orderId = orderRes.data?.id;

    // Step 2: If MOMO, initiate payment and redirect
    if (paymentMethod === 'MOMO') {
      showToast('Đang kết nối MoMo...', 'info');
      const momoRes = await api.payment.momoCreate(orderId);
      if (momoRes.success && momoRes.data?.payUrl) {
        window.location.href = momoRes.data.payUrl; // redirect to MoMo
        return;
      } else {
        showToast(momoRes.message || 'Không thể kết nối MoMo', 'error');
        setLoading(btn, false);
        return;
      }
    }

    // Step 3: COD / BANK_TRANSFER — normal success flow
    showToast('Đặt hàng thành công!');
    closeCheckoutModal();
    setTimeout(() => window.location.href = `order-detail.html?id=${orderId}`, 1000);
  } catch (err) {
    showToast(err.message || 'Lỗi đặt hàng', 'error');
    setLoading(btn, false);
  }
}
```

---

### Step 4 — Add `api.payment` to `api.js`

```js
payment: {
  momoCreate: (orderId) => apiFetch('/payment/momo/create', {
    method: 'POST',
    body: JSON.stringify({ orderId }),
  }),
},
```

Add this block inside the `const api = { ... }` object.

---

### Step 5 — Create `payment-result.html`

**Path:** `frontend/payment-result.html`

New page for handling both success and failure after MoMo redirect.

```html
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <title>Kết quả thanh toán — MAISON</title>
  <link rel="stylesheet" href="styles.css">
  <link rel="stylesheet" href="shared.css">
</head>
<body>
<nav class="navbar" id="navbar">
  <!-- same navbar as other pages -->
</nav>

<div class="container" style="padding-top:120px;padding-bottom:4rem;text-align:center;max-width:500px;margin:0 auto">
  <div id="resultContent">
    <div class="skeleton" style="height:120px;border-radius:16px;margin-bottom:1rem"></div>
  </div>
</div>

<script src="js/api.js"></script>
<script>
const params = new URLSearchParams(window.location.search);
const status  = params.get('status');
const orderId = params.get('orderId');

document.addEventListener('DOMContentLoaded', () => {
  renderNavActions();
  const el = document.getElementById('resultContent');

  if (status === 'success') {
    el.innerHTML = `
      <div style="font-size:4rem;margin-bottom:1rem">✅</div>
      <h1 style="font-size:1.75rem;font-weight:800;margin-bottom:0.5rem">Thanh toán thành công!</h1>
      <p style="color:var(--color-text-muted);margin-bottom:2rem">Đơn hàng của bạn đã được xác nhận và đang được xử lý.</p>
      <div style="display:flex;gap:1rem;justify-content:center;flex-wrap:wrap">
        <a href="order-detail.html?id=${orderId}" class="btn btn-dark">Xem đơn hàng</a>
        <a href="products.html" class="btn btn-outline">Tiếp tục mua sắm</a>
      </div>`;
  } else {
    el.innerHTML = `
      <div style="font-size:4rem;margin-bottom:1rem">❌</div>
      <h1 style="font-size:1.75rem;font-weight:800;margin-bottom:0.5rem">Thanh toán thất bại</h1>
      <p style="color:var(--color-text-muted);margin-bottom:2rem">Giao dịch không thành công hoặc đã bị hủy.</p>
      <div style="display:flex;gap:1rem;justify-content:center;flex-wrap:wrap">
        <a href="order-detail.html?id=${orderId}" class="btn btn-outline">Xem đơn hàng</a>
        <a href="products.html" class="btn btn-dark">Tiếp tục mua sắm</a>
      </div>`;
  }
});
</script>
</body>
</html>
```

---

### Step 6 — Show MoMo badge in order-detail.html

In `order-detail.html`, when rendering order, show payment status:

```js
// Add in order detail render (after status badge)
${o.paymentMethod === 'MOMO' ? `
  <span style="display:inline-flex;align-items:center;gap:6px;font-size:0.8rem;padding:3px 10px;background:#f3e8ff;color:#6b21a8;border-radius:20px;border:1px solid #d8b4fe">
    💜 MoMo · ${o.paymentStatus === 'PAID' ? 'Đã thanh toán' : o.paymentStatus === 'FAILED' ? 'Thất bại' : 'Chờ thanh toán'}
  </span>
` : ''}
```

---

## API additions needed in OrderResponse

The `OrderResponse` needs to include `paymentStatus` and `paymentMethod` fields so the frontend can display them. Check if `paymentMethod` is already in `OrderResponse.java` — if not, add:

```java
private String paymentMethod;
private String paymentStatus;
```

And populate in `OrderResponse.from()`:
```java
.paymentMethod(order.getPaymentMethod())
.paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "N_A")
```

---

## ngrok Setup (for local IPN testing)

MoMo cannot call `localhost:8080`. Use ngrok to expose local server:

```bash
# Install ngrok (one-time)
# Windows: winget install ngrok.ngrok
# Or download from ngrok.com

# Expose local backend
ngrok http 8080

# Copy the https URL (e.g. https://abc123.ngrok-free.app)
# Set in application.yml or environment:
# MOMO_IPN_URL=https://abc123.ngrok-free.app/api/payment/momo/ipn
# MOMO_REDIRECT_URL=https://abc123.ngrok-free.app/api/payment/momo/return
```

---

## Success Criteria

- [ ] Checkout modal shows "Ví MoMo" option with info banner
- [ ] Selecting MOMO + "Đặt hàng" redirects to MoMo sandbox payment page
- [ ] Sandbox payment test completes (phone: any 10-digit, OTP: 000000)
- [ ] After payment, user redirected to `payment-result.html?status=success`
- [ ] IPN received → order status = CONFIRMED in database
- [ ] Failed payment shows `payment-result.html?status=failed` + order CANCELLED
- [ ] Order detail shows "💜 MoMo · Đã thanh toán" badge

## Notes

- **IPN vs redirectUrl:** Both fire after payment. IPN is reliable (server→server). redirectUrl is browser-based (may fail if user closes tab). Backend should handle both but trust IPN more.
- **Amount:** Use `order.getFinalAmount()` (after all discounts/points), not `totalAmount`
- **Sandbox test card:** ATM `9704 0000 0000 0018`, OTP: OTP received on test app
