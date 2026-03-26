# Research Report 02 — Advanced Technologies (AI, Cloudinary, Mail, Redis, Swagger)

## 1. Google Gemini API — Java/Spring Boot Integration

### SDK:
```xml
<!-- Google AI Java SDK (official) -->
com.google.ai.client.generativeai:google-ai-java:0.9.0
<!-- OR use Spring AI (Spring Boot 3.2+) -->
org.springframework.ai:spring-ai-vertex-ai-gemini-spring-boot-starter:1.0.0-M6
```

### Use Cases for Fashion E-Commerce:
| Feature | Gemini Prompt |
|---------|--------------|
| Auto-generate product description | `"Write a 100-word fashion product description for: {name}, color: {color}, material: {material}"` |
| Fashion chatbot | `"You are a fashion assistant. Answer: {userMessage}. Context: {productList}"` |
| Smart search suggestions | `"Suggest 5 search queries for fashion: {partialQuery}"` |
| Outfit recommendations | `"Recommend outfit combinations for: {productName}"` |

### Config pattern:
```yaml
gemini:
  api-key: ${GEMINI_API_KEY}
  model: gemini-1.5-flash  # free tier
  max-tokens: 500
```

### Service pattern: `GeminiService.generateContent(String prompt)` → `String`

---

## 2. Cloudinary — Image Upload in Spring Boot

### Dependency:
```xml
com.cloudinary:cloudinary-http44:1.38.0
```

### Config:
```yaml
cloudinary:
  cloud-name: ${CLOUDINARY_CLOUD_NAME}
  api-key: ${CLOUDINARY_API_KEY}
  api-secret: ${CLOUDINARY_API_SECRET}
```

### Upload flow:
1. Controller accepts `MultipartFile`
2. `CloudinaryService.upload(file)` → `Map result` → `result.get("url")`
3. Store URL string in `product_images` table
4. Return public URL in response DTO

### Free tier: 25GB storage, 25GB bandwidth/month — sufficient for academic project.

---

## 3. Spring Mail — Forgot Password / Reset Password Flow

### Dependency: `spring-boot-starter-mail`

### Email providers (free):
- **Gmail SMTP**: host=smtp.gmail.com, port=587, TLS — use App Password
- **Brevo (formerly Sendinblue)**: 300 emails/day free, more reliable for production

### Reset Password Flow:
1. POST /api/auth/forgot-password `{email}` → generate UUID token → save to `password_reset_tokens` table (expires in 15min) → send email with link
2. GET /api/auth/verify-reset-token?token={uuid} → validate expiry
3. POST /api/auth/reset-password `{token, newPassword}` → update hash, invalidate token

### Token table: `password_reset_tokens(id, userId, token, expiresAt, used)`

---

## 4. Redis Caching — Spring Boot

### Dependencies:
```xml
spring-boot-starter-data-redis
spring-boot-starter-cache
```

### Config:
```yaml
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: 6379
```

### Cache strategy for e-commerce:
```java
@Cacheable(value = "products", key = "#id")         // Get product by ID
@CacheEvict(value = "products", key = "#id")         // After update/delete
@Cacheable(value = "categories", key = "'all'")      // All categories list
@Cacheable(value = "products", key = "#filter.hashCode()") // Filtered products
```

**Free Redis hosting:** Upstash (10K commands/day free) — perfect for academic project.

---

## 5. Swagger / OpenAPI 3 — Spring Boot 3.x

### Dependency:
```xml
org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0
```

### Config:
```yaml
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha
```

### Annotations:
- `@Tag(name = "Auth")` on controller
- `@Operation(summary = "Login user")` on endpoint
- `@ApiResponse(responseCode = "200", description = "Success")`
- `@SecurityRequirement(name = "bearerAuth")` for protected endpoints

### Security scheme config: `@SecurityScheme(name = "bearerAuth", type = HTTP, scheme = "bearer", bearerFormat = "JWT")`

---

## Summary — Tech Stack Final Recommendation

| Layer | Tech | Reason |
|-------|------|--------|
| Framework | Spring Boot 3.2 + Java 21 | LTS, virtual threads |
| Database | PostgreSQL on Neon.tech | Free, reliable |
| Migrations | Flyway | Simple SQL-based |
| Auth | JWT + Spring OAuth2 | Standard |
| Images | Cloudinary | Free 25GB |
| Cache | Redis on Upstash | Free tier |
| AI | Gemini 1.5 Flash | Free API |
| Email | Gmail SMTP / Brevo | Free |
| Docs | Springdoc OpenAPI | Auto-gen |
| Deploy | Render.com | Free tier |
