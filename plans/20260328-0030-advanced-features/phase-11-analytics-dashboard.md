# Phase 11 — Analytics Dashboard (Enhanced)

**Complexity:** Hard
**Depends on:** Phase 08 (loyalty points data), Phase 09 (referral data), Phase 10 (inventory data)
**Blocks:** Nothing

---

## Overview

Enhance the existing admin dashboard (`/api/admin/dashboard`) with:
1. Revenue breakdown by category, payment method, coupon usage.
2. Top products by revenue and by units sold.
3. Customer acquisition chart (new users per day, referral vs organic).
4. Loyalty points ledger summary (total issued, redeemed, expired).
5. Excel export of any dashboard table.
6. Date range filter on all charts.

**Current state:** `DashboardService` returns fixed 30-day window with no filters. One `GET /api/admin/dashboard` endpoint returns everything in one blob.

---

## Key Insights

- Do NOT split into dozens of micro-endpoints (YAGNI). Use a few focused endpoints with query params.
- Excel export uses Apache POI (add to pom.xml) — returns `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.
- Chart data computation is expensive — cache in Redis with TTL 5 min (admin cache, separate from public product cache).
- Frontend charts: use Chart.js (CDN) already referenced or add it — no build step needed.
- Date range defaults: last 30 days. Max range: 365 days (prevent full table scans).

---

## pom.xml Addition Required

```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

---

## New API Endpoints (additive — existing `/api/admin/dashboard` unchanged)

```
GET  /api/admin/analytics/revenue            — revenue by day/week/month with date range
GET  /api/admin/analytics/top-products       — top N products by revenue or units
GET  /api/admin/analytics/customers          — new users per day + referral vs organic
GET  /api/admin/analytics/loyalty            — total points issued/redeemed/expired
GET  /api/admin/analytics/inventory-summary  — current stock overview
GET  /api/admin/analytics/export/orders      — Excel download (date range filter)
GET  /api/admin/analytics/export/products    — Excel download
GET  /api/admin/analytics/export/customers   — Excel download
```

Query params for all: `?from=2026-01-01&to=2026-03-28&granularity=daily`

---

## Files to Create (Backend)

| File | Purpose |
|------|---------|
| `service/AnalyticsService.java` | All analytics queries + caching |
| `controller/AnalyticsController.java` | Admin-only endpoints |
| `service/ExcelExportService.java` | Apache POI workbook generation |
| `dto/response/AnalyticsResponse.java` | Nested response objects for each metric |

## Files to Modify (Backend)

| File | Change |
|------|--------|
| `repository/OrderRepository.java` | Add revenue by category, by payment method queries |
| `repository/OrderItemRepository.java` | Add top products by revenue/units queries |
| `repository/UserRepository.java` | Add new users per day query |
| `repository/LoyaltyTransactionRepository.java` | Add summary aggregation query |
| `config/RedisConfig.java` | Add `analytics` cache with 5 min TTL |

## Files to Modify (Frontend)

| File | Change |
|------|--------|
| `static/admin/dashboard.html` | Add date range picker, new chart sections, export buttons |

---

## Key Repository Queries

### Revenue by category (OrderRepository)
```java
@Query("SELECT p.category.name, COALESCE(SUM(oi.price * oi.quantity), 0) " +
       "FROM OrderItem oi JOIN oi.order o JOIN oi.product p " +
       "WHERE o.status = 'DELIVERED' AND o.createdAt BETWEEN :from AND :to " +
       "GROUP BY p.category.name ORDER BY SUM(oi.price * oi.quantity) DESC")
List<Object[]> revenueByCategory(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
```

### Top products by revenue
```java
@Query("SELECT p.id, p.name, COALESCE(SUM(oi.price * oi.quantity), 0) as rev, SUM(oi.quantity) as units " +
       "FROM OrderItem oi JOIN oi.product p JOIN oi.order o " +
       "WHERE o.status = 'DELIVERED' AND o.createdAt BETWEEN :from AND :to " +
       "GROUP BY p.id, p.name ORDER BY rev DESC")
List<Object[]> topProductsByRevenue(Pageable pageable, LocalDateTime from, LocalDateTime to);
```

### New users per day
```java
@Query("SELECT DATE(u.createdAt), COUNT(u) FROM User u " +
       "WHERE u.createdAt BETWEEN :from AND :to AND u.deletedAt IS NULL " +
       "GROUP BY DATE(u.createdAt) ORDER BY DATE(u.createdAt)")
List<Object[]> newUsersByDay(LocalDateTime from, LocalDateTime to);
```

---

## ExcelExportService Pattern

```java
public byte[] exportOrders(LocalDateTime from, LocalDateTime to) {
    try (Workbook workbook = new XSSFWorkbook()) {
        Sheet sheet = workbook.createSheet("Orders");

        // Header row
        Row header = sheet.createRow(0);
        String[] columns = {"ID", "Customer", "Status", "Total", "Date"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
        }

        // Data rows
        List<Order> orders = orderRepo.findByCreatedAtBetween(from, to);
        int rowNum = 1;
        for (Order o : orders) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(o.getId());
            row.createCell(1).setCellValue(o.getUser().getName());
            // ...
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }
}
```

Controller response:
```java
@GetMapping("/export/orders")
public ResponseEntity<byte[]> exportOrders(...) {
    byte[] data = excelExportService.exportOrders(from, to);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders.xlsx")
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(data);
}
```

---

## Frontend Enhancements

### Date Range Picker
Use native `<input type="date">` — no external library needed. Two inputs: `from` and `to`. Default: last 30 days. On change: refetch all charts.

### Chart.js Integration
```html
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
```

New charts to add to `admin/dashboard.html`:
1. Revenue by category — Doughnut chart
2. Top 10 products by revenue — Horizontal bar chart
3. New users per day — Line chart
4. Payment method distribution — Pie chart

### Export Buttons
```html
<button onclick="exportData('orders')">Xuất Excel đơn hàng</button>
<!-- Triggers: window.location = `/api/admin/analytics/export/orders?from=...&to=...` -->
```

---

## Caching Strategy

```java
@Cacheable(value = "analytics", key = "'revenue:' + #from + ':' + #to")
public AnalyticsResponse.Revenue getRevenue(LocalDateTime from, LocalDateTime to) { ... }
```

Cache `analytics` in `RedisConfig`:
```java
RedisCacheConfiguration analyticsConfig = RedisCacheConfiguration.defaultCacheConfig()
    .entryTtl(Duration.ofMinutes(5));
```

---

## Implementation Steps

1. Add Apache POI dependency to `pom.xml`.
2. Add `analytics` cache config to `RedisConfig`.
3. Add new query methods to `OrderRepository`, `OrderItemRepository`, `UserRepository`.
4. Create `AnalyticsResponse` DTO with nested records for each metric.
5. Create `AnalyticsService` — each metric method with `@Cacheable`.
6. Create `ExcelExportService` — 3 export methods (orders, products, customers).
7. Create `AnalyticsController` — all admin analytics endpoints.
8. Add Chart.js CDN to `admin/dashboard.html`.
9. Add date range picker UI (two `<input type="date">` + "Apply" button).
10. Add revenue-by-category doughnut chart section.
11. Add top-10-products horizontal bar chart.
12. Add new-users line chart.
13. Add export buttons with `window.location` href download.
14. Test: export generates valid `.xlsx` with correct data.

---

## Success Criteria

- [ ] Revenue chart responds to date range filter changes.
- [ ] Top 10 products sorted correctly by revenue.
- [ ] New users chart shows day-by-day acquisition.
- [ ] Excel export downloads valid `.xlsx` file.
- [ ] Analytics data cached — repeated requests within 5 min do not hit DB.
- [ ] All charts render without errors on `admin/dashboard.html`.
- [ ] Date range >365 days returns 400 Bad Request.

---

## Risk Assessment

| Risk | Mitigation |
|------|-----------|
| Large date ranges causing slow queries | Max 365 day validation in controller; DB indexes on `created_at` already exist |
| Apache POI memory usage with large exports | Stream rows using `SXSSFWorkbook` (streaming variant) for >10k rows |
| Cache invalidation — stale analytics after new order | Analytics cache TTL is 5 min; acceptable for admin reporting |
