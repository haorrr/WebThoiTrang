# Phase 05 — Quick Buy Popup

**Complexity:** Easy
**Depends on:** Phase 01 (variants), Phase 03 (flash price in response)
**Blocks:** Nothing

---

## Overview

A modal popup on the product list page (`products.html`) that lets users select a variant and add to cart without navigating to product detail. Pure frontend feature — no new backend endpoints needed. Reuses existing `GET /api/products/{id}` and `POST /api/cart/items`.

---

## Key Insights

- Zero new backend code required — product detail endpoint already returns variants.
- The popup fetches product detail when user clicks "Quick Buy" button.
- Quantity selector + variant picker inside modal → "Add to Cart" calls existing cart API.
- Mobile-friendly: full-screen bottom sheet on mobile, centered modal on desktop.

---

## Files to Create (Frontend)

| File | Purpose |
|------|---------|
| `static/js/quick-buy.js` | Modal logic: fetch product, render variants, add to cart |

## Files to Modify (Frontend)

| File | Change |
|------|--------|
| `static/products.html` | Add "Quick Buy" button on each product card; include quick-buy.js |
| `static/styles.css` | Modal overlay + animation styles |
| `static/shared.css` | Add modal component styles (reusable) |

---

## Quick Buy Modal HTML Structure

```html
<!-- Injected into body by quick-buy.js -->
<div id="quick-buy-overlay" class="modal-overlay hidden">
  <div class="quick-buy-modal">
    <button class="modal-close">&times;</button>
    <div class="modal-image"><img id="qb-image" /></div>
    <div class="modal-info">
      <h2 id="qb-name"></h2>
      <div id="qb-price"></div>        <!-- includes flash price if active -->
      <div id="qb-sizes"></div>        <!-- size buttons -->
      <div id="qb-colors"></div>       <!-- color swatches -->
      <div id="qb-quantity">
        <button id="qb-qty-minus">-</button>
        <span id="qb-qty">1</span>
        <button id="qb-qty-plus">+</button>
      </div>
      <button id="qb-add-to-cart">Thêm vào giỏ</button>
      <a id="qb-detail-link">Xem chi tiết →</a>
    </div>
  </div>
</div>
```

---

## quick-buy.js Logic

```javascript
async function openQuickBuy(productId) {
    const product = await api.getProduct(productId);   // GET /api/products/{id}
    renderModal(product);  // populate name, price, flashPrice, variants
    showModal();
}

async function addToCart() {
    const variantId = getSelectedVariantId();
    const qty = getQuantity();
    await api.addToCart({ productId, variantId, quantity: qty });
    showSuccessToast("Đã thêm vào giỏ hàng!");
    closeModal();
    updateCartBadge();
}
```

---

## Implementation Steps

1. Create `quick-buy.js` — modal creation, variant selection state, add-to-cart call.
2. Add "Quick Buy" button to product card template in `products.html`.
3. On button click: call `openQuickBuy(productId)`.
4. Add modal CSS to `shared.css` (overlay, slide-up animation, responsive).
5. Handle loading state (spinner while fetching product detail).
6. Show flash price badge inside modal if `product.flashPrice` is set.
7. Test: keyboard `Escape` closes modal; clicking overlay closes modal.

---

## Success Criteria

- [ ] "Quick Buy" button visible on each product card.
- [ ] Clicking opens modal within 300ms (product fetched, rendered).
- [ ] Selecting size + color updates available stock display.
- [ ] "Add to Cart" works; cart badge increments.
- [ ] Flash sale price shown in modal when active.
- [ ] Modal closes on Escape key and overlay click.
- [ ] No new backend endpoints needed.
