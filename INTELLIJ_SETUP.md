# Hướng dẫn Setup & Chạy dự án trên IntelliJ IDEA

## Yêu cầu cài đặt trước

| Phần mềm | Phiên bản | Tải về |
|----------|-----------|--------|
| JDK | 22 | https://www.oracle.com/java/technologies/downloads/#java22 |
| IntelliJ IDEA | 2023.x+ (Community hoặc Ultimate) | https://www.jetbrains.com/idea/download |
| Docker Desktop | Mới nhất | https://www.docker.com/products/docker-desktop |
| Maven | 3.9+ | https://maven.apache.org/download.cgi |

> **Ghi chú:** Nếu dùng IntelliJ **Community** thì chạy PostgreSQL + Redis qua Docker.
> Nếu dùng **Ultimate** thì có thể dùng Database tool tích hợp sẵn.

---

## Bước 1 — Mở project trong IntelliJ

1. Mở IntelliJ IDEA
2. Chọn **File → Open** → chọn thư mục `WebThoiTrang`
3. IntelliJ hỏi *"Trust this project?"* → chọn **Trust Project**
4. Chờ IntelliJ **import Maven** và tải dependencies (xem progress bar dưới cùng)

---

## Bước 2 — Cài đặt JDK trong IntelliJ

1. Vào **File → Project Structure** (`Ctrl + Alt + Shift + S`)
2. Tab **Project → SDK** → chọn **JDK 22**
   - Nếu chưa có: nhấn **Add SDK → Download SDK** → chọn **OpenJDK 22**
3. **Language Level** → chọn **22**
4. Nhấn **Apply → OK**

---

## Bước 3 — Khởi động PostgreSQL và Redis bằng Docker

Mở **Terminal** trong IntelliJ (`Alt + F12`) và chạy:

```bash
docker-compose up -d
```

Kiểm tra đã chạy thành công:

```bash
docker-compose ps
```

Kết quả mong đợi:
```
NAME                  STATUS
fashionshop-postgres  Up (healthy)
fashionshop-redis     Up (healthy)
```

> PostgreSQL chạy tại `localhost:5432`, Redis tại `localhost:6379`

---

## Bước 4 — Tạo file `.env`

Copy file `.env.example` → tạo file `.env` ở thư mục gốc project:

```bash
cp .env.example .env
```

Mở file `.env` và điền các giá trị **tối thiểu** để chạy được:

```env
SPRING_PROFILES_ACTIVE=dev
DB_USERNAME=postgres
DB_PASSWORD=password
JWT_SECRET=mySecretKeyThatIsAtLeast32CharactersLongForSecurity
```

Các biến còn lại (Google OAuth, Cloudinary, Gemini, Gmail SMTP) có thể bỏ trống nếu chưa cần test tính năng đó.

---

## Bước 5 — Cấu hình Run Configuration trong IntelliJ

### Cách 1: Dùng EnvFile plugin (khuyến nghị)

1. Vào **File → Settings → Plugins** (`Ctrl + Alt + S`)
2. Tìm plugin **"EnvFile"** → cài đặt → restart IntelliJ
3. Mở **Run → Edit Configurations...**
4. Chọn `FashionShopApplication` (nếu chưa có thì nhấn **+** → **Spring Boot**)
5. Tab **EnvFile** → tích chọn **Enable EnvFile**
6. Nhấn **+** → chọn file `.env` vừa tạo
7. Nhấn **Apply → OK**

### Cách 2: Nhập tay Environment Variables

1. Mở **Run → Edit Configurations...**
2. Chọn hoặc tạo **Spring Boot** configuration:
   - **Main class:** `com.fashionshop.FashionShopApplication`
   - **Module:** `fashion-shop`
3. Nhấn vào ô **Environment variables** → nhấn icon bên phải
4. Thêm từng biến:

   | Name | Value |
   |------|-------|
   | `SPRING_PROFILES_ACTIVE` | `dev` |
   | `DB_USERNAME` | `postgres` |
   | `DB_PASSWORD` | `password` |
   | `JWT_SECRET` | `mySecretKeyThatIsAtLeast32CharactersLongForSecurity` |

5. Nhấn **OK → Apply → OK**

---

## Bước 6 — Chạy ứng dụng

### Qua Run Configuration vừa tạo:
- Nhấn nút **Run** (▶) hoặc `Shift + F10`

### Qua class main trực tiếp:
- Mở file `src/main/java/com/fashionshop/FashionShopApplication.java`
- Nhấn icon ▶ xanh bên trái `public static void main`
- Chọn **Run 'FashionShopApplication'**

### Qua Terminal:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## Bước 7 — Kiểm tra ứng dụng đã chạy

Ứng dụng khởi động thành công khi thấy log:

```
Started FashionShopApplication in X.XXX seconds
```

Mở trình duyệt kiểm tra:

| URL | Mô tả |
|-----|-------|
| http://localhost:8080/swagger-ui.html | Swagger UI — thử API |
| http://localhost:8080/actuator/health | Health check |
| http://localhost:8080/api-docs | OpenAPI JSON spec |

---

## Bước 8 — Test API nhanh bằng Swagger

1. Mở http://localhost:8080/swagger-ui.html
2. Gọi **POST /api/auth/register** để tạo tài khoản
3. Gọi **POST /api/auth/login** → copy `accessToken` từ response
4. Nhấn nút **Authorize** (🔒) góc phải màn hình
5. Dán token vào ô: `Bearer {accessToken}`
6. Bây giờ có thể gọi tất cả API cần xác thực

> **Tài khoản ADMIN mặc định:**
> Email: `admin@fashionshop.com`
> Password: `Admin@123`

---

## Flyway Migration

Khi khởi động lần đầu, Flyway tự động tạo toàn bộ database schema từ file:
```
src/main/resources/db/migration/V1__init.sql
```

Nếu muốn reset database sạch (dev only):
```bash
# Xóa và tạo lại database
docker-compose down -v
docker-compose up -d
```
Rồi restart ứng dụng — Flyway sẽ chạy lại migration từ đầu.

---

## Cấu hình tính năng tùy chọn

### Google OAuth2
1. Vào https://console.cloud.google.com → tạo project
2. **APIs & Services → Credentials → Create OAuth Client ID**
3. Application type: **Web application**
4. Authorized redirect URIs: `http://localhost:8080/login/oauth2/code/google`
5. Copy **Client ID** và **Client Secret** vào `.env`

### GitHub OAuth2
1. Vào https://github.com/settings/developers → **New OAuth App**
2. Authorization callback URL: `http://localhost:8080/login/oauth2/code/github`
3. Copy **Client ID** và **Client Secret** vào `.env`

### Cloudinary (upload ảnh)
1. Đăng ký tại https://cloudinary.com (free tier: 25GB)
2. Vào **Dashboard** → copy **Cloud Name**, **API Key**, **API Secret**
3. Điền vào `.env`

### Gmail SMTP (gửi email reset password)
1. Bật **2-Factor Authentication** trên Google Account
2. Vào **Google Account → Security → App Passwords**
3. Tạo App Password cho **Mail**
4. Điền email và password 16 ký tự vào `.env`

### Google Gemini AI
1. Vào https://aistudio.google.com
2. Nhấn **Get API key → Create API key**
3. Điền vào `.env`

---

## Xử lý lỗi thường gặp

### `Connection refused: localhost:5432`
→ Docker chưa chạy. Chạy lại: `docker-compose up -d`

### `Flyway migration failed`
→ Database đang có dữ liệu cũ conflict. Reset:
```bash
docker-compose down -v && docker-compose up -d
```

### `Redis connection refused`
→ Redis chưa chạy hoặc port 6379 bị chiếm.
Tạm thời disable Redis bằng cách thêm vào `.env`:
```env
SPRING_CACHE_TYPE=none
```

### `Port 8080 already in use`
→ Đổi port trong `.env`:
```env
PORT=8081
```

### Build lỗi `Cannot find symbol`
→ Chạy lại Maven:
**IntelliJ → Maven panel (bên phải) → Reload All Maven Projects** (icon ↺)

---

## Cấu trúc project tóm tắt

```
src/main/java/com/fashionshop/
├── config/          # Security, Redis, OpenAPI, CORS, Cloudinary
├── controller/      # REST endpoints (Auth, User, Product, Order, ...)
├── dto/             # Request/Response DTOs
│   ├── request/
│   └── response/
├── entity/          # JPA Entities (User, Product, Order, ...)
├── exception/       # Custom exceptions + GlobalExceptionHandler
├── repository/      # Spring Data JPA repositories
├── security/        # JWT filter, OAuth2 handlers
├── service/         # Business logic
└── util/            # SlugUtil

src/main/resources/
├── application.yml          # Cấu hình chung
├── application-dev.yml      # Cấu hình local
├── application-prod.yml     # Cấu hình production
└── db/migration/
    └── V1__init.sql         # Tạo toàn bộ schema + seed data
```
