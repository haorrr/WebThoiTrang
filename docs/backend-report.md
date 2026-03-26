# FashionShop Backend — Báo cáo chức năng

**Ngày:** 26/03/2026
**Dự án:** Fashion E-Commerce REST API (J2EE Academic Project)
**Tech stack:** Spring Boot 3.2.5 · Java 22 · PostgreSQL · Redis · Flyway
**Tổng số file Java:** 102 files · ~5,200 dòng code

---

## 1. Tổng quan kiến trúc

```
src/main/java/com/fashionshop/
├── controller/     11 files  — REST endpoints
├── service/        13 files  — Business logic
├── repository/     12 files  — Data access (JPA + custom JPQL)
├── entity/         11 files  — JPA entities
├── dto/
│   ├── request/    18 files  — Input validation DTOs
│   └── response/   17 files  — Output DTOs
├── config/          6 files  — Spring config beans
├── security/        3 files  — JWT filter, UserDetails
├── security/oauth2  4 files  — OAuth2 handlers
├── exception/       4 files  — Custom exceptions + global handler
└── util/            1 file   — SlugUtil
```

**Database:** 10 bảng, migrations quản lý bằng Flyway
**Deployment:** Docker multi-stage · render.yaml cho Render.com

---

## 2. Database Schema

| Bảng | Mô tả | Ghi chú |
|------|-------|---------|
| `users` | Tài khoản người dùng | Soft delete, role ADMIN/USER, OAuth2 provider |
| `categories` | Danh mục sản phẩm | Tự tham chiếu (parent_id), cây phân cấp |
| `products` | Sản phẩm | Soft delete, slug, ai_description, stock |
| `product_images` | Ảnh sản phẩm | Lưu URL Cloudinary, isPrimary, sortOrder |
| `coupons` | Mã giảm giá | PERCENT/FIXED, min order, max uses, expiry |
| `orders` | Đơn hàng | Status state machine, coupon snapshot |
| `order_items` | Chi tiết đơn hàng | Snapshot giá tại thời điểm mua |
| `reviews` | Đánh giá sản phẩm | PENDING→APPROVED/REJECTED, unique user+product |
| `cart_items` | Giỏ hàng | Lưu tạm theo user |
| `password_reset_tokens` | Token đặt lại mật khẩu | Expiry, one-time use |

---

## 3. API Endpoints — 60 endpoints tổng cộng

### 3.1 Authentication — `/api/auth`

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| POST | `/register` | Đăng ký tài khoản mới | Public |
| POST | `/login` | Đăng nhập, trả về access + refresh token | Public |
| POST | `/refresh` | Làm mới access token bằng refresh token | Public |
| POST | `/logout` | Đăng xuất, xóa refresh token | Public |
| POST | `/forgot-password` | Gửi email link đặt lại mật khẩu | Public |
| GET | `/verify-reset-token` | Kiểm tra token reset có hợp lệ | Public |
| POST | `/reset-password` | Đặt lại mật khẩu bằng token | Public |

**OAuth2 Social Login:**
- Google: `GET /oauth2/authorization/google`
- GitHub: `GET /oauth2/authorization/github`
- Callback redirect: `{frontend}/oauth2/callback?accessToken=...&refreshToken=...`

---

### 3.2 Users — `/api/users`

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| GET | `/` | Danh sách user (search, filter status, sort, phân trang) | ADMIN |
| GET | `/me` | Thông tin cá nhân đang đăng nhập | User |
| PUT | `/me` | Cập nhật thông tin cá nhân | User |
| GET | `/{id}` | Xem chi tiết user | ADMIN |
| PUT | `/{id}` | Cập nhật user | ADMIN |
| DELETE | `/{id}` | Xóa mềm user | ADMIN |
| PATCH | `/{id}/status` | Bật/tắt trạng thái ACTIVE/INACTIVE | ADMIN |

---

### 3.3 Categories — `/api/categories`

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| GET | `/` | Lấy cây danh mục (tree structure) | Public |
| GET | `/{id}` | Xem chi tiết danh mục | Public |
| POST | `/` | Tạo danh mục (hỗ trợ parent_id) | ADMIN |
| PUT | `/{id}` | Cập nhật danh mục | ADMIN |
| DELETE | `/{id}` | Xóa mềm (kiểm tra còn sản phẩm không) | ADMIN |
| PATCH | `/{id}/status` | Bật/tắt trạng thái | ADMIN |

---

### 3.4 Products — `/api/products`

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| GET | `/` | Danh sách sản phẩm (search, filter category/price/status, sort, phân trang) | Public |
| GET | `/{id}` | Xem chi tiết sản phẩm | Public |
| GET | `/slug/{slug}` | Xem sản phẩm theo slug | Public |
| POST | `/` | Tạo sản phẩm | ADMIN |
| PUT | `/{id}` | Cập nhật sản phẩm | ADMIN |
| DELETE | `/{id}` | Xóa mềm | ADMIN |
| PATCH | `/{id}/status` | Bật/tắt trạng thái | ADMIN |
| POST | `/{id}/images` | Upload ảnh sản phẩm (Cloudinary) | ADMIN |
| DELETE | `/{id}/images/{imageId}` | Xóa ảnh sản phẩm | ADMIN |
| POST | `/{id}/ai-description` | Tự động sinh mô tả bằng Gemini AI | ADMIN |

---

### 3.5 Cart — `/api/cart`

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| GET | `/` | Xem giỏ hàng + tổng tiền | User |
| POST | `/items` | Thêm sản phẩm vào giỏ (kiểm tra stock) | User |
| PUT | `/items/{itemId}` | Cập nhật số lượng | User |
| DELETE | `/items/{itemId}` | Xóa 1 sản phẩm khỏi giỏ | User |
| DELETE | `/` | Xóa toàn bộ giỏ hàng | User |

---

### 3.6 Orders — `/api/orders`

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| POST | `/` | Tạo đơn hàng từ giỏ (atomic transaction) | User |
| GET | `/` | Danh sách đơn hàng của tôi (filter status) | User |
| GET | `/{id}` | Xem chi tiết đơn hàng | User/ADMIN |
| POST | `/{id}/cancel` | Hủy đơn (hoàn stock, trừ coupon) | User |
| GET | `/admin/all` | Tất cả đơn hàng (filter status, sort) | ADMIN |
| PATCH | `/admin/{id}/status` | Cập nhật trạng thái đơn hàng | ADMIN |

**Order state machine:**
```
PENDING → CONFIRMED → SHIPPING → DELIVERED
    ↓           ↓
CANCELLED   CANCELLED
```

---

### 3.7 Reviews — `/api/reviews`

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| GET | `/product/{productId}` | Đánh giá đã duyệt của sản phẩm | Public |
| GET | `/my` | Đánh giá của tôi | User |
| POST | `/` | Tạo đánh giá (1 user chỉ review 1 lần/sản phẩm) | User |
| PUT | `/{id}` | Sửa đánh giá (tự động reset về PENDING) | User |
| DELETE | `/{id}` | Xóa mềm đánh giá | User/ADMIN |
| GET | `/admin/all` | Tất cả đánh giá (filter status) | ADMIN |
| PATCH | `/admin/{id}/moderate` | Duyệt/từ chối đánh giá | ADMIN |

**Review state machine:** `PENDING → APPROVED / REJECTED`

---

### 3.8 Coupons — `/api/coupons`

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| POST | `/validate` | Kiểm tra mã giảm giá + tính số tiền giảm | User |
| GET | `/` | Danh sách tất cả coupon | ADMIN |
| GET | `/{id}` | Chi tiết coupon | ADMIN |
| POST | `/` | Tạo coupon | ADMIN |
| PUT | `/{id}` | Cập nhật coupon | ADMIN |
| DELETE | `/{id}` | Xóa mềm coupon | ADMIN |
| PATCH | `/{id}/toggle` | Bật/tắt coupon | ADMIN |

**Loại coupon:** `PERCENT` (%) và `FIXED` (VNĐ cố định)
**Điều kiện:** min order amount, max uses, expiry date

---

### 3.9 AI (Gemini) — `/api/ai`

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| POST | `/chat` | Chat với AI trợ lý thời trang | Public |
| POST | `/generate-description` | Sinh mô tả sản phẩm tự động | Public |
| GET | `/recommendations` | Gợi ý sản phẩm theo sở thích + ngân sách | Public |

**Model:** Gemini 1.5 Flash
**Ngôn ngữ:** Tiếng Việt

---

### 3.10 Upload — `/api/upload`

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| POST | `/image` | Upload ảnh lên Cloudinary | ADMIN |

**Hỗ trợ:** JPEG, PNG, WebP, GIF — tối đa 10MB

---

### 3.11 Admin Dashboard — `/api/admin/dashboard`

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| GET | `/` | Tổng quan toàn bộ hệ thống | ADMIN |

**Response bao gồm:**
- **Doanh thu:** tổng, tháng này, tháng trước, % tăng trưởng
- **Đơn hàng:** tổng, tháng này, đếm từng trạng thái
- **Người dùng:** tổng, mới tháng này, đang active
- **Sản phẩm:** tổng, hết hàng, sắp hết hàng (≤5)
- **Biểu đồ doanh thu:** theo ngày, 30 ngày gần nhất
- **Biểu đồ danh mục:** số sản phẩm theo từng category
- **Bảng sản phẩm sắp hết hàng** (top 10)
- **Bảng đơn hàng gần nhất** (10 đơn mới nhất)

---

## 4. Tính năng xuyên suốt

### 4.1 Bảo mật

| Tính năng | Chi tiết |
|-----------|---------|
| **JWT Authentication** | Access token 15 phút, RS256 + HMAC |
| **Refresh Token Rotation** | UUID token, BCrypt hash lưu DB, xoay vòng mỗi lần dùng |
| **OAuth2 Social Login** | Google + GitHub, upsert user theo provider |
| **Role-based Authorization** | `ROLE_ADMIN` / `ROLE_USER` qua `@PreAuthorize` |
| **Password Security** | BCrypt strength 10 |
| **Forgot/Reset Password** | Token 1 lần, hết hạn 1 giờ, gửi qua email |
| **Soft Delete** | `deleted_at` field, `@Where(deleted_at IS NULL)` |
| **CORS** | Whitelist localhost:3000, 5173, 4200 + APP_FRONTEND_URL |

### 4.2 Xử lý dữ liệu

| Tính năng | Chi tiết |
|-----------|---------|
| **Phân trang** | `Page<T>` với page/size/sort, whitelist sort fields chống injection |
| **Tìm kiếm** | `JpaSpecificationExecutor` + dynamic `Specification`, ILIKE case-insensitive |
| **Slug tự động** | `SlugUtil` hỗ trợ Unicode NFD (tiếng Việt), append timestamp khi trùng |
| **Snapshot giá** | OrderItem lưu giá tại thời điểm đặt hàng |
| **Stock management** | Kiểm tra trước khi đặt, giảm khi tạo đơn, hoàn lại khi hủy |

### 4.3 Performance & Cache

| Tính năng | Chi tiết |
|-----------|---------|
| **Redis Cache** | `@Cacheable` trên product detail, category tree |
| **TTL** | Products: 10 phút, Categories: 30 phút |
| **Cache Eviction** | `@CacheEvict` trên tất cả write operations |
| **N+1 Prevention** | `@EntityGraph` trên product/order queries |
| **Connection Pool** | HikariCP, max 5 connections (dev) |

### 4.4 Tích hợp dịch vụ ngoài

| Dịch vụ | Chức năng |
|---------|-----------|
| **Cloudinary** | Upload/delete ảnh, validate MIME type + 10MB limit |
| **Google Gemini 1.5 Flash** | Chat trợ lý, sinh mô tả sản phẩm, gợi ý sản phẩm |
| **Gmail SMTP** | Email đặt lại mật khẩu HTML (async, non-blocking) |
| **PostgreSQL (Neon.tech)** | Production database |
| **Redis (Upstash)** | Production cache |

### 4.5 Developer Experience

| Tính năng | Chi tiết |
|-----------|---------|
| **Swagger UI** | `/swagger-ui.html` — tất cả 60 endpoints, JWT auth tích hợp |
| **OpenAPI Spec** | `/api-docs` — JSON spec cho codegen |
| **Flyway Migration** | V1: schema + seed admin, V2: fix column type |
| **Spring Profiles** | `dev` (local Docker) / `prod` (Neon.tech + Upstash) |
| **Health Check** | `/actuator/health` cho monitoring |
| **Global Exception Handler** | Chuẩn hóa tất cả lỗi về `ApiResponse` format |

---

## 5. Cấu trúc Response chuẩn

Tất cả API trả về dạng:

```json
{
  "success": true,
  "message": "Success",
  "data": { ... },
  "timestamp": "2026-03-26T17:40:00"
}
```

Lỗi validation trả về thêm field `errors`:
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "email": "Email không hợp lệ",
    "password": "Mật khẩu tối thiểu 6 ký tự"
  }
}
```

---

## 6. Deployment

| Môi trường | Config |
|------------|--------|
| **Local** | Docker Compose (PostgreSQL 16 + Redis 7) |
| **Production** | Render.com (Docker) + Neon.tech PostgreSQL + Upstash Redis |
| **CI/CD** | Push GitHub → Render auto-deploy từ `render.yaml` |
| **Docker** | Multi-stage build, eclipse-temurin:22, non-root user |

**Env vars cần thiết cho production:** 16 biến (xem `.env.example`)

---

## 7. Tóm tắt số liệu

| Hạng mục | Số lượng |
|----------|---------|
| Tổng file Java | 102 |
| Tổng dòng code | ~5,200 |
| REST Endpoints | 60 |
| Database tables | 10 |
| Flyway migrations | 2 |
| Commits | 4 |
| Dependencies chính | 15+ |
