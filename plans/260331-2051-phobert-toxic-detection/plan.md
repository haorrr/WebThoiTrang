# PhoBERT Toxic Review Detection — Implementation Plan

**Created:** 2026-03-31
**Priority:** Medium
**Estimated effort:** 10h total (3 phases)

## Goal

Auto-detect toxic Vietnamese reviews using a PhoBERT Python microservice; surface toxicity scores in admin UI; admin-toggleable via `system_config`.

## Architecture

```
User POST /api/reviews
        │
        ▼
[Spring Boot] ──save PENDING──► [PostgreSQL: reviews.toxic_score]
        │
        │ async WebClient (5s timeout, non-blocking)
        ▼
[toxic-detector:8000 FastAPI]
  POST /classify → { toxic: bool, score: float, label: string }
        │
        ├─ !toxic  → UPDATE status=APPROVED, save toxic_score
        ├─ toxic   → keep PENDING, save toxic_score (admin reviews manually)
        └─ timeout/down → auto-APPROVED (graceful degradation)
```

## Phases

| # | Phase | Status | File |
|---|-------|--------|------|
| 1 | Python FastAPI microservice + Docker | 🔲 pending | [phase-01-python-service.md](phase-01-python-service.md) |
| 2 | Spring Boot: DB migration, async call, toggle | 🔲 pending | [phase-02-backend-integration.md](phase-02-backend-integration.md) |
| 3 | Admin UI: dashboard toggle + reviews toxic badge | 🔲 pending | [phase-03-admin-ui.md](phase-03-admin-ui.md) |

## Key Decisions

- **Model**: `jesse-tong/vietnamese_hate_speech_detection_phobert` (HuggingFace, ready-to-use, ~75-85% F1 on Vietnamese)
- **Word segmentation**: VNCoreNLP via `py_vncorenlp` — required for PhoBERT accuracy (+10-15% F1)
- **WebClient** (spring-webflux dep added) — async, non-blocking; 5s timeout + fallback approve
- **system_config** keys: `toxic_detection.enabled` (bool), `toxic_detection.threshold` (default `0.70`)
- **Cache**: `@Cacheable("system_config")` 60s TTL via existing RedisCacheManager
- **toxic_score** `DECIMAL(5,4)` nullable — `null` means unscored or feature was off
- Model pre-baked into Docker image at build time (not downloaded at runtime)
- Dashboard toggle reuses `SECURITY_KEYS` array pattern (zero new JS boilerplate)

## Dependencies (ordering)

Phase 01 → Phase 02 → Phase 03
Each phase is independently testable.

## Resource Requirements

- Docker image: ~2.5GB (model 550MB + torch CPU 600MB)
- RAM: 1.5–2GB for Python container (CPU inference)
- Inference latency: ~1–3s per review on 2 vCPU
- `start_period: 90s` in healthcheck — Spring Boot defaults to approve during warmup
