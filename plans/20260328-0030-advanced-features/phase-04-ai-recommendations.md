# Phase 04 — AI Recommendations (Gemini-based)

**Complexity:** Medium
**Depends on:** Phase 01 (variant data enriches context), existing `GeminiService`
**Blocks:** Nothing

---

## Overview

Enhance the existing `/api/ai/recommendations` endpoint (currently takes free-text preferences + budget) with actual purchase/view history context. Add a view tracking mechanism, then pass history to Gemini as structured context. Also add a "similar products" endpoint that uses category + price range.

**Current state:** `GeminiService.getRecommendations(preferences, budget)` — no history.
**Target:** Pass user's last 5 purchased categories + last 5 viewed products to Gemini prompt.

---

## Key Insights

- Do NOT build a collaborative filtering ML model (YAGNI). Gemini does the intelligence.
- View history stored in Redis (TTL 7 days) — no DB table needed for views. Simple, fast.
- Purchase history comes from existing `OrderItem` records — already in DB.
- "Similar products" does NOT use AI — simple category + price range query (performance).
- Recommendation results cached in Redis per user (TTL 30 min) — Gemini is slow.

---

## Redis Data Structures

```
Key: "product:views:{userId}"  → Redis List, max 20 entries, LPUSH + LTRIM
Value: productId (String)
TTL: 7 days

Key: "recommendations:{userId}" → String (JSON)
TTL: 30 minutes (invalidated when user places an order)
```

No DB migration required.

---

## Files to Create (Backend)

| File | Purpose |
|------|---------|
| `service/ViewTrackingService.java` | Push productId to Redis list on view; get recent views |
| `dto/response/RecommendationResponse.java` | List of ProductSummaryResponse + AI explanation text |

## Files to Modify (Backend)

| File | Change |
|------|--------|
| `service/GeminiService.java` | New `getPersonalizedRecommendations(userId, purchaseHistory, viewHistory)` method |
| `controller/AiController.java` | Update `/recommendations` to accept userId from JWT; call view tracker |
| `controller/ProductController.java` | Add `POST /api/products/{id}/view` endpoint (fire-and-forget) |
| `service/OrderService.java` | On order created, evict `recommendations:{userId}` cache |
| `config/RedisConfig.java` | Ensure `RedisTemplate<String, String>` bean available |

---

## API Endpoints

```
POST /api/products/{id}/view          — track product view (User, optional; anonymous skipped)
GET  /api/ai/recommendations          — personalized recommendations (User, uses JWT userId)
GET  /api/products/{id}/similar       — similar products by category+price (Public, no AI)
```

---

## GeminiService Enhancement

```java
public String getPersonalizedRecommendations(
        List<String> recentlyViewedProductNames,
        List<String> purchasedCategories,
        String budget) {

    String context = String.format("""
        Người dùng gần đây đã xem: %s.
        Họ đã mua sản phẩm thuộc danh mục: %s.
        Ngân sách: %s VNĐ.
        Hãy gợi ý 3-5 sản phẩm thời trang phù hợp từ FashionShop, kèm lý do ngắn gọn.
        """,
        String.join(", ", recentlyViewedProductNames),
        String.join(", ", purchasedCategories),
        budget);
    return chat(context);
}
```

---

## ViewTrackingService

```java
@Service
public class ViewTrackingService {
    private final RedisTemplate<String, String> redisTemplate;
    private static final int MAX_VIEWS = 20;
    private static final long TTL_DAYS = 7;

    public void trackView(Long userId, Long productId) {
        String key = "product:views:" + userId;
        redisTemplate.opsForList().leftPush(key, productId.toString());
        redisTemplate.opsForList().trim(key, 0, MAX_VIEWS - 1);
        redisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS);
    }

    public List<Long> getRecentViews(Long userId) {
        // Returns list of productIds
    }
}
```

---

## Implementation Steps

1. Create `ViewTrackingService` using existing `RedisTemplate`.
2. Add `POST /api/products/{id}/view` in `ProductController` — calls `trackView()` async (non-blocking, no error thrown on failure).
3. Enhance `GeminiService` with `getPersonalizedRecommendations()`.
4. Update `AiController.getRecommendations()`:
   - Extract userId from JWT (if authenticated).
   - Load recent viewed product names from Redis.
   - Load top 5 purchased categories from `OrderItemRepository`.
   - Check Redis cache for `recommendations:{userId}`; return if hit.
   - Call Gemini, cache result.
5. Add `GET /api/products/{id}/similar` in `ProductController` — query by same category, price ±30%, limit 6.
6. In `OrderService.createOrder()`, evict `recommendations:{userId}` cache.
7. Frontend: On `product-detail.html` page load, call `POST /api/products/{id}/view` (fire-and-forget, ignore errors).
8. Frontend: Add "You might also like" section on product detail using `/similar` endpoint.
9. Frontend: On profile or home page, show personalized recommendations from AI endpoint.

---

## New Repository Query Needed

In `OrderItemRepository`:
```java
@Query("SELECT oi.product.category.name, COUNT(oi) FROM OrderItem oi " +
       "WHERE oi.order.user.id = :userId AND oi.order.status = 'DELIVERED' " +
       "GROUP BY oi.product.category.name ORDER BY COUNT(oi) DESC")
List<Object[]> findTopCategoriesByUser(@Param("userId") Long userId, Pageable pageable);
```

---

## Success Criteria

- [ ] Viewing a product while logged in pushes to Redis view history.
- [ ] `/api/ai/recommendations` for a user who bought "Áo" returns relevant suggestions.
- [ ] Recommendations are cached; second call within 30 min returns instantly.
- [ ] Similar products section shows 6 products from same category.
- [ ] Anonymous users get generic recommendations (no history context).

---

## Risk Assessment

| Risk | Mitigation |
|------|-----------|
| Gemini latency (1-3s) making recommendations slow | Redis cache 30 min; lazy-load section on frontend |
| Redis unavailable → view tracking fails | Wrap trackView in try/catch, log warning, don't rethrow |
| User with no purchase history gets poor recommendations | Fall back to popular categories / trending prompt |
