# Advanced Features Implementation Plan

**Date:** 2026-03-28
**Project:** FashionShop Spring Boot Backend + Vanilla Frontend
**Status:** Planning

---

## Overview

11 advanced features grouped into 4 domains. Ordered by dependency — each phase builds on the previous.

**Codebase baseline:** 102 Java files, 60 endpoints, 10 DB tables, Flyway migrations V1+V2.

---

## Implementation Order (Dependency-first)

| Phase | Feature | Complexity | Depends On |
|-------|---------|------------|------------|
| 01 | Size/Color Variants (ProductVariant) | Medium | — (foundational) |
| 02 | Wishlist | Easy | Phase 01 |
| 03 | Flash Sale | Medium | Phase 01 |
| 04 | AI Recommendations | Medium | Phase 01, existing GeminiService |
| 05 | Quick Buy Popup | Easy | Phase 01, 03 |
| 06 | Order Tracking (enhanced) | Easy | existing Order state machine |
| 07 | Email Automation | Medium | Phase 06, existing EmailService |
| 08 | Loyalty Points | Medium | Phase 07 |
| 09 | Referral System | Medium | Phase 08 |
| 10 | Inventory Management | Medium | Phase 01 |
| 11 | Analytics Dashboard (enhanced) | Hard | Phase 08, 09, 10 |

**Total estimated effort:** ~15–20 developer-days

---

## Phase Files

- [Phase 01 — Product Variants (Size/Color)](./phase-01-product-variants.md)
- [Phase 02 — Wishlist](./phase-02-wishlist.md)
- [Phase 03 — Flash Sale](./phase-03-flash-sale.md)
- [Phase 04 — AI Recommendations](./phase-04-ai-recommendations.md)
- [Phase 05 — Quick Buy Popup](./phase-05-quick-buy.md)
- [Phase 06 — Order Tracking Enhanced](./phase-06-order-tracking.md)
- [Phase 07 — Email Automation](./phase-07-email-automation.md)
- [Phase 08 — Loyalty Points](./phase-08-loyalty-points.md)
- [Phase 09 — Referral System](./phase-09-referral-system.md)
- [Phase 10 — Inventory Management](./phase-10-inventory-management.md)
- [Phase 11 — Analytics Dashboard Enhanced](./phase-11-analytics-dashboard.md)

---

## Key Architectural Decisions

1. **ProductVariant table** replaces the ad-hoc `size`/`color` string fields in `cart_items` and `order_items` — critical foundation for accurate stock per variant.
2. **Flyway migration** for every new table — no Hibernate DDL auto.
3. **Spring `@Scheduled`** for Flash Sale activation/deactivation and abandoned cart detection (already have `@Async` in EmailService).
4. **Redis** reused for Flash Sale price cache and recommendation cache — no new infrastructure.
5. **No new external services** — Excel export uses Apache POI (add to pom.xml), all else uses existing stack.

---

## New pom.xml Dependencies Required

```xml
<!-- Apache POI (Excel export - Phase 11) -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

All other features use existing dependencies.
