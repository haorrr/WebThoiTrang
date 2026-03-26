# Cài Đặt Môi Trường — Fashion E-Commerce Spring Boot

**Cập nhật:** 2026-03-26

---

## 1. Java 21 (JDK)

**Tải:** https://adoptium.net/temurin/releases/?version=21

- Chọn **Temurin 21 (LTS)** → Windows x64 → `.msi`
- Cài đặt → Next → Next → Finish
- Kiểm tra:
```bash
java -version
# eclipse-temurin version 21...
```

---

## 2. Maven

**Tải:** https://maven.apache.org/download.cgi → `apache-maven-3.9.x-bin.zip`

- Giải nén vào `C:\Program Files\Maven`
- Thêm vào **System Environment Variables**:
  - `MAVEN_HOME` = `C:\Program Files\Maven\apache-maven-3.9.x`
  - Thêm vào `Path`: `%MAVEN_HOME%\bin`
- Kiểm tra:
```bash
mvn -version
# Apache Maven 3.9.x
```

> **Tip:** Nếu dùng IntelliJ thì Maven đã có sẵn built-in, không cần cài tay.

---

## 3. IDE — IntelliJ IDEA (Khuyến nghị)

**Tải:** https://www.jetbrains.com/idea/download → **Community Edition** (miễn phí)

- Cài đặt → Next → chọn **Add to PATH** → Install
- Plugins cần cài trong IntelliJ:
  - **Lombok** (bắt buộc)
  - **Spring Boot** (thường có sẵn)
  - **Database Navigator** (xem PostgreSQL)

> Hoặc dùng **VS Code** + Extension Pack for Java + Spring Boot Extension Pack

---

## 4. Git

**Tải:** https://git-scm.com/download/win → `Git-2.x.x-64-bit.exe`

- Cài đặt → Next liên tục → Finish
- Cấu hình sau khi cài:
```bash
git config --global user.name "Tên của bạn"
git config --global user.email "email@example.com"
```
- Kiểm tra:
```bash
git --version
# git version 2.x.x
```

---

## 5. Docker Desktop

**Tải:** https://www.docker.com/products/docker-desktop → Windows

- Cài đặt → yêu cầu **WSL2** (Windows Subsystem for Linux)
- Nếu chưa có WSL2:
```powershell
wsl --install
# Restart máy sau khi cài
```
- Kiểm tra:
```bash
docker --version
docker-compose --version
```

> Dùng để chạy PostgreSQL và Redis local thay vì cài trực tiếp.

---

## 6. PostgreSQL (Local Dev — qua Docker)

Không cần cài PostgreSQL tay. Chạy qua Docker:

```bash
docker run --name postgres-dev \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=fashionshop \
  -p 5432:5432 \
  -d postgres:16
```

**Hoặc** dùng file `docker-compose.yml` của project (Phase 1 sẽ tạo).

**GUI để xem database:** Cài **DBeaver** (miễn phí):
- Tải: https://dbeaver.io/download → Windows Installer
- Kết nối: Host=localhost, Port=5432, DB=fashionshop, User=postgres, Pass=password

---

## 7. Redis (Local Dev — qua Docker)

```bash
docker run --name redis-dev \
  -p 6379:6379 \
  -d redis:7-alpine
```

> Không cần cài Redis tay, Docker là đủ cho dev local.

---

## 8. Postman (Test API)

**Tải:** https://www.postman.com/downloads → Windows 64-bit

- Cài đặt → Đăng ký tài khoản miễn phí (hoặc Skip)
- Dùng để test tất cả các REST API endpoint
- Có thể import Swagger JSON từ `/api-docs` vào Postman

> Hoặc dùng **Bruno** (nhẹ hơn, offline): https://www.usebruno.com

---

## 9. Tài Khoản Cần Tạo (Miễn Phí)

| Dịch vụ | Link đăng ký | Dùng để |
|---------|-------------|---------|
| **Neon.tech** | https://neon.tech | PostgreSQL cloud (production) |
| **Upstash** | https://upstash.com | Redis cloud (production) |
| **Cloudinary** | https://cloudinary.com | Lưu trữ ảnh sản phẩm |
| **Google Cloud Console** | https://console.cloud.google.com | Lấy OAuth2 Client ID/Secret cho đăng nhập Google |
| **GitHub** | https://github.com | Lấy OAuth2 App cho đăng nhập GitHub + lưu code |
| **Render.com** | https://render.com | Deploy backend lên cloud |
| **Google AI Studio** | https://aistudio.google.com | Lấy Gemini API Key |
| **Gmail** | Dùng tài khoản có sẵn | SMTP gửi email reset mật khẩu |

---

## 10. Cấu Hình Google OAuth2

1. Vào https://console.cloud.google.com → Tạo project mới
2. **APIs & Services** → **Credentials** → **Create Credentials** → **OAuth 2.0 Client ID**
3. Application type: **Web application**
4. Authorized redirect URIs: `http://localhost:8080/login/oauth2/code/google`
5. Copy **Client ID** và **Client Secret** → dán vào `application.yml`

---

## 11. Cấu Hình GitHub OAuth2

1. GitHub → Settings → Developer settings → **OAuth Apps** → New OAuth App
2. Homepage URL: `http://localhost:8080`
3. Authorization callback URL: `http://localhost:8080/login/oauth2/code/github`
4. Copy **Client ID** và **Client Secret** → dán vào `application.yml`

---

## 12. Lấy Gmail App Password (cho gửi email)

1. Tài khoản Google → Security → **2-Step Verification** (bật lên)
2. Sau khi bật → tìm **App passwords**
3. Select app: **Mail** → Select device: **Windows Computer** → Generate
4. Copy 16 ký tự password → dán vào `MAIL_PASSWORD` trong `.env`

---

## Checklist Trước Khi Code

```
[ ] Java 21 cài xong — java -version ✓
[ ] Maven cài xong — mvn -version ✓
[ ] IntelliJ mở được, plugin Lombok đã cài
[ ] Git cài xong — git --version ✓
[ ] Docker Desktop đang chạy
[ ] PostgreSQL container đang chạy (port 5432)
[ ] Redis container đang chạy (port 6379)
[ ] Postman cài xong
[ ] Tài khoản Neon.tech tạo xong, có connection string
[ ] Tài khoản Cloudinary tạo xong, có API key
[ ] Google OAuth2 credentials tạo xong
[ ] GitHub OAuth App tạo xong
[ ] Gemini API key lấy xong từ AI Studio
[ ] Gmail App Password tạo xong
```

---

## Khởi Động Nhanh Local (sau khi cài xong)

```bash
# 1. Khởi động PostgreSQL + Redis
docker run -d --name postgres-dev -e POSTGRES_PASSWORD=password -e POSTGRES_DB=fashionshop -p 5432:5432 postgres:16
docker run -d --name redis-dev -p 6379:6379 redis:7-alpine

# 2. Clone project
git clone https://github.com/<your-username>/fashion-shop-api.git
cd fashion-shop-api

# 3. Copy file env mẫu
cp .env.example .env
# Điền các API key vào file .env

# 4. Chạy project
mvn spring-boot:run

# 5. Mở Swagger UI
# http://localhost:8080/swagger-ui.html
```
