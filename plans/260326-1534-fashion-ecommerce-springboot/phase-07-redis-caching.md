# Phase 07 — Redis Caching & Performance

## Context Links

- [plan.md](plan.md)
- [Research 02 — Redis/Upstash & Caching Strategy](research/researcher-02-report.md)
- Depends on: Phase 04 (ProductService, CategoryService), Phase 06 (AiController — for rate limiting)

---

## Overview

| Field | Value |
|-------|-------|
| Date | 2026-03-26 |
| Priority | MEDIUM |
| Implementation Status | PENDING |
| Review Status | PENDING |
| Description | Add Redis caching (Upstash free tier) for product listings and category tree to reduce DB hits. Add simple in-memory rate limiting for AI endpoints. Cache eviction on CRUD operations. |

---

## Key Insights

- Upstash Redis free tier: 10,000 commands/day — enough for academic demo (product listings + category tree = ~20 cache hits per page load).
- Use Spring's `@Cacheable` / `@CacheEvict` annotations — no custom Redis code needed (KISS).
- Category tree is globally shared (no per-user state) → single cache key `"categories::all"`, TTL 10 min.
- Product listing cache key must encode all filter params → use `#root.methodName + #filters.hashCode()` or build a deterministic cache key string.
- Rate limiting for AI endpoints: simple in-memory `Map<String, AtomicInteger>` per IP per minute — NOT Redis-based (avoids complexity; YAGNI for academic scope).
- Do NOT cache cart or order data — these are user-specific and frequently mutated.

---

## Requirements

### Functional
- Category tree cached; invalidated on any category create/update/delete
- Product listing (filtered) cached; invalidated on any product create/update/delete/status change
- Product by ID cached; invalidated on update/delete of that product
- AI `/api/ai/**` endpoints rate limited to 10 requests/minute per IP
- AI product context (`getTopProductsForContext`) cached with 5 min TTL

### Technical
- Redis connection to Upstash via `rediss://` (SSL) URL
- Default cache TTL: 10 minutes
- Cache names: `"categories"`, `"products"`, `"ai-context"`
- On Upstash free tier, configure `maxmemory-policy allkeys-lru` (already default on Upstash)

---

## Architecture

### Package Structure
```
com.fashionshop/
├── config/
│   └── RedisConfig.java
└── util/
    └── RateLimiter.java   (simple in-memory, not Redis-based)
```

### Cache Keys Design
```
categories::all                    → getCategoryTree()
products::{hashCode}               → getProducts(filters)
products::{id}                     → getProductById(id)
products::slug::{slug}             → getProductBySlug(slug)
ai-context::top                    → getTopProductsForContext()
```

### Cache Eviction Strategy
```
On createProduct / updateProduct / deleteProduct / toggleProductStatus:
  → @CacheEvict(value = "products", allEntries = true)

On createCategory / updateCategory / deleteCategory / toggleCategoryStatus:
  → @CacheEvict(value = "categories", allEntries = true)
```

---

## Related Code Files

### Create
- `src/main/java/com/fashionshop/config/RedisConfig.java`
- `src/main/java/com/fashionshop/util/RateLimiter.java`

### Modify
- `src/main/java/com/fashionshop/service/CategoryService.java` — add `@Cacheable` / `@CacheEvict`
- `src/main/java/com/fashionshop/service/ProductService.java` — add `@Cacheable` / `@CacheEvict`
- `src/main/java/com/fashionshop/controller/AiController.java` — inject `RateLimiter`, check before processing
- `src/main/resources/application.yml` — add Redis + cache config
- `src/main/resources/application-prod.yml` — add Upstash Redis URL

---

## Implementation Steps

1. **Verify dependencies** are already in `pom.xml` (added in Phase 1):
   - `spring-boot-starter-data-redis`
   - `spring-boot-starter-cache`

2. **Add Redis config to `application.yml`**:
   ```yaml
   spring:
     redis:
       host: ${REDIS_HOST:localhost}
       port: ${REDIS_PORT:6379}
       password: ${REDIS_PASSWORD:}
       ssl:
         enabled: ${REDIS_SSL:false}
     cache:
       type: redis
       redis:
         time-to-live: 600000   # 10 minutes in ms
         cache-null-values: false
   ```

3. **Add Upstash config to `application-prod.yml`**:
   ```yaml
   spring:
     redis:
       host: ${REDIS_HOST}
       port: ${REDIS_PORT:6379}
       password: ${REDIS_PASSWORD}
       ssl:
         enabled: true
   ```

4. **Create `RedisConfig.java`**:
   ```java
   @Configuration
   @EnableCaching
   public class RedisConfig {

     @Bean
     public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
       RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
         .entryTtl(Duration.ofMinutes(10))
         .disableCachingNullValues()
         .serializeValuesWith(RedisSerializationContext.SerializationPair
           .fromSerializer(new GenericJackson2JsonRedisSerializer()));

       Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
       cacheConfigs.put("ai-context", config.entryTtl(Duration.ofMinutes(5)));
       cacheConfigs.put("categories", config.entryTtl(Duration.ofMinutes(10)));
       cacheConfigs.put("products", config.entryTtl(Duration.ofMinutes(10)));

       return RedisCacheManager.builder(factory)
         .cacheDefaults(config)
         .withInitialCacheConfigurations(cacheConfigs)
         .build();
     }
   }
   ```
   Note: `@EnableCaching` can go here or on `FashionShopApplication`.

5. **Ensure all cached DTOs implement `Serializable`**: `CategoryTreeResponse`, `ProductSummaryResponse`, `ProductResponse` — add `implements Serializable`.

6. **Add caching to `CategoryService`**:
   ```java
   @Cacheable(value = "categories", key = "'all'")
   public List<CategoryTreeResponse> getCategoryTree() { ... }

   @CacheEvict(value = "categories", allEntries = true)
   public CategoryResponse createCategory(CategoryRequest req) { ... }

   @CacheEvict(value = "categories", allEntries = true)
   public CategoryResponse updateCategory(Long id, CategoryRequest req) { ... }

   @CacheEvict(value = "categories", allEntries = true)
   public void deleteCategory(Long id) { ... }

   @CacheEvict(value = "categories", allEntries = true)
   public CategoryResponse toggleStatus(Long id) { ... }
   ```

7. **Add caching to `ProductService`**:
   ```java
   // Product by ID
   @Cacheable(value = "products", key = "'id:' + #id")
   public ProductResponse getProductById(Long id) { ... }

   @Cacheable(value = "products", key = "'slug:' + #slug")
   public ProductResponse getProductBySlug(String slug) { ... }

   // Evict on any mutation
   @CacheEvict(value = "products", allEntries = true)
   public ProductResponse createProduct(ProductRequest req) { ... }

   @CacheEvict(value = "products", allEntries = true)
   public ProductResponse updateProduct(Long id, ProductRequest req) { ... }

   @CacheEvict(value = "products", allEntries = true)
   public void deleteProduct(Long id) { ... }

   @CacheEvict(value = "products", allEntries = true)
   public ProductResponse toggleStatus(Long id) { ... }

   // AI context cache
   @Cacheable(value = "ai-context", key = "'top'")
   public List<String> getTopProductsForContext() { ... }
   ```

   Note: `getProducts(filters, pageable)` is NOT cached with `@Cacheable` due to complex Pageable key — the in-memory Spring cache with Redis handles it fine at academic scale without this optimization. Add a TODO for production if needed.

8. **Create `RateLimiter`** (in-memory, per IP):
   ```java
   @Component
   public class RateLimiter {
     private final Map<String, List<Long>> requestLog = new ConcurrentHashMap<>();
     private static final int MAX_REQUESTS = 10;
     private static final long WINDOW_MS = 60_000L;

     public boolean isAllowed(String ip) {
       long now = System.currentTimeMillis();
       requestLog.compute(ip, (k, timestamps) -> {
         if (timestamps == null) timestamps = new ArrayList<>();
         timestamps.removeIf(t -> now - t > WINDOW_MS);
         return timestamps;
       });
       List<Long> timestamps = requestLog.get(ip);
       if (timestamps.size() >= MAX_REQUESTS) return false;
       timestamps.add(now);
       return true;
     }
   }
   ```

9. **Inject `RateLimiter` into `AiController`**:
   ```java
   @PostMapping("/chat")
   public ResponseEntity<ApiResponse<ChatResponse>> chat(
       HttpServletRequest request,
       @Valid @RequestBody ChatRequest req) {
     String ip = request.getRemoteAddr();
     if (!rateLimiter.isAllowed(ip)) {
       return ResponseEntity.status(429)
         .body(ApiResponse.error("Too many requests. Please wait a minute."));
     }
     // ... existing logic
   }
   ```
   Apply same pattern to `/recommendations` and `/search-suggestions`.

10. **Test Redis caching**:
    - Call `GET /api/categories` twice → check logs: second call has no SQL query
    - Call `POST /api/categories` (create) → call `GET /api/categories` → cache invalidated, fresh data
    - Simulate AI rate limit: call `/api/ai/chat` 11 times rapidly → 11th should return 429

---

## Todo List

- [ ] Add Redis + cache config to `application.yml` and `application-prod.yml`
- [ ] Create `RedisConfig.java` with `RedisCacheManager`
- [ ] Make all cached response DTOs `Serializable`
- [ ] Add `@Cacheable` / `@CacheEvict` to `CategoryService` (5 annotations)
- [ ] Add `@Cacheable` / `@CacheEvict` to `ProductService` (6 annotations)
- [ ] Create `RateLimiter` component
- [ ] Inject `RateLimiter` into `AiController` (3 endpoints)
- [ ] Add `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_SSL` to `.env.example`
- [ ] Test cache hit/miss via SQL log
- [ ] Test cache eviction on CRUD
- [ ] Test AI rate limiting (10 req/min)
- [ ] Verify Upstash connection in prod profile

---

## Success Criteria

- `GET /api/categories` second call: no DB query in logs (cache hit)
- `POST /api/categories` then `GET /api/categories`: fresh data (cache evicted)
- 11th AI request within 60s → 429 response
- Application starts correctly with Redis unavailable in dev (configure `spring.cache.type=simple` as fallback in dev profile)
- `GET /api/products/{id}` cached; after `PUT /api/products/{id}` → next GET fetches from DB

---

## Risk Assessment

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Upstash free tier connection string format differs | Medium | Use `RedisURI` with `rediss://` scheme; set `ssl.enabled=true` |
| Cached DTO not Serializable → `SerializationException` | High | Add `implements Serializable` to ALL response DTOs before enabling cache |
| `@CacheEvict` not triggered if method throws exception | Low | `@CacheEvict` has `beforeInvocation=false` by default — evict happens AFTER success, which is correct |
| In-memory `RateLimiter` doesn't work behind load balancer | Low | Acceptable for academic; note in docs |
| `getProducts()` cache with complex Pageable key causing stale data | Medium | Skip caching `getProducts()` list — only cache by ID/slug |

---

## Security Considerations

- Redis password (`REDIS_PASSWORD`) must be set in production — Upstash provides this automatically
- Rate limiter uses `request.getRemoteAddr()` — if behind proxy/Render, get real IP from `X-Forwarded-For` header
- Do NOT cache sensitive user data (cart, orders, profile) — only cache public product/category data

---

## Next Steps

- Phase 8 (Deployment) — `REDIS_HOST`, `REDIS_PASSWORD`, `REDIS_SSL` must be set as Render.com env vars
- No further phases depend on this — this is a pure enhancement layer
