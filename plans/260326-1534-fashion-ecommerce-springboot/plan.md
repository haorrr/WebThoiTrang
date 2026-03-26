# Fashion E-Commerce Spring Boot — Implementation Plan

**Date:** 2026-03-26
**Project:** Fashion shop REST API backend (J2EE academic project)
**Status:** PLANNING

---

## Tech Stack

| Layer | Tech |
|-------|------|
| Framework | Spring Boot 3.2, Java 21 |
| Database | PostgreSQL / Neon.tech |
| Migrations | Flyway |
| Auth | JWT (jjwt 0.12.3) + Spring OAuth2 (Google/GitHub) |
| Images | Cloudinary |
| Cache | Redis / Upstash |
| AI | Google Gemini 1.5 Flash |
| Email | Spring Mail + Gmail SMTP |
| Docs | Springdoc OpenAPI 2.3 |
| Deploy | Docker + Render.com |

---

## Phases

| # | Phase | Priority | Status |
|---|-------|----------|--------|
| 1 | [Project Setup & Core Infrastructure](phase-01-project-setup.md) | HIGH | PENDING |
| 2 | [Authentication (JWT + OAuth2 + Email)](phase-02-authentication.md) | HIGH | PENDING |
| 3 | [User Management (Admin)](phase-03-user-management.md) | HIGH | PENDING |
| 4 | [Category & Product CRUD](phase-04-category-product.md) | HIGH | PENDING |
| 5 | [Cart, Orders, Reviews, Coupons](phase-05-cart-orders-reviews-coupons.md) | HIGH | PENDING |
| 6 | [AI (Gemini) Integration](phase-06-ai-gemini.md) | MEDIUM | PENDING |
| 7 | [Redis Caching & Performance](phase-07-redis-caching.md) | MEDIUM | PENDING |
| 8 | [Swagger, Testing & Deployment](phase-08-swagger-deployment.md) | HIGH | PENDING |

---

## Research Reports

- [researcher-01-report.md](research/researcher-01-report.md) — Spring Boot architecture, DB hosting, Flyway, Render deploy
- [researcher-02-report.md](research/researcher-02-report.md) — Gemini AI, Cloudinary, Spring Mail, Redis, Swagger

---

## Key Dependencies Between Phases

- Phase 2 depends on Phase 1 (project skeleton + DB)
- Phases 3–5 depend on Phase 2 (security context)
- Phase 6 depends on Phase 4 (products exist)
- Phase 7 depends on Phases 4–5 (data to cache)
- Phase 8 depends on all prior phases

---

## Package Root

`com.fashionshop` — all source under `src/main/java/com/fashionshop/`
